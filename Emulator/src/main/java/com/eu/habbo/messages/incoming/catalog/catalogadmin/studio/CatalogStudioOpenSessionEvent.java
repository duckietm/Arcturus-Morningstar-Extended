package com.eu.habbo.messages.incoming.catalog.catalogadmin.studio;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeEntry;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeOperation;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogEntityType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogOfferSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogPageSnapshot;
import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogStudioSessionState;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogVersionSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.JdbcCatalogLiveSnapshotRepository;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioPublishedVersion;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio.CatalogStudioSessionComposer;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog Studio session open.
 *
 * <p>The client may send the revision it already holds. When nothing changed since then the answer is a tiny
 * delta with no entities; when only a few operations happened the answer carries every page (cheap) plus the
 * offers touched by those operations and the ids of the deleted ones, read from the change journal. Only a client
 * without a snapshot (or a catalog that changed too much) receives the full 90k-offer snapshot, and that encoded
 * snapshot is cached per revision so re-opening the studio does not re-serialize the whole catalog.
 */
public final class CatalogStudioOpenSessionEvent extends CatalogStudioEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogStudioOpenSessionEvent.class);
    private static final String CHANGES_SINCE_SQL =
            "SELECT changes_json FROM catalog_manager_history WHERE revision > ? ORDER BY revision";
    /** Above this many touched offers a full snapshot is cheaper than a delta. */
    private static final int MAX_DELTA_OFFERS = 5000;
    /** Full snapshot encoded for one revision; re-opens at the same revision reuse it. */
    private static final AtomicReference<CachedSnapshot> CACHE = new AtomicReference<>();

    private record CachedSnapshot(long revision, List<String> chunks, int pageCount, int offerCount) {}

    private record DeltaPayload(
            long baseRevision,
            List<CatalogPageSnapshot> pages,
            List<CatalogOfferSnapshot> offers,
            List<DeletedOffer> deletedOffers) {}

    private record DeletedOffer(String catalogType, int offerId) {}

    /** Called when the catalog tables were changed outside the studio (RCON updatecatalog, imports). */
    public static void invalidateSnapshotCache() {
        CACHE.set(null);
    }

    @Override
    public void handle() {
        if (!authorize()) return;

        long knownRevision = this.packet.bytesAvailable() >= 4 ? Math.max(0, this.packet.readInt()) : 0;

        // Loading ~90k offers and composing a multi-megabyte response must never
        // occupy a GamePacketHandler thread: that stalls room joins/login packets
        // for every connection assigned to the same executor.
        Emulator.getThreading().run(() -> this.openSession(knownRevision));
    }

    private void openSession(long knownRevision) {
        try {
            CatalogStudioSessionState state = studio().queries().loadSession();
            long revision = state.revision();
            List<CatalogStudioPublishedVersion> versions = state.publishedVersions().stream()
                    .map(version -> new CatalogStudioPublishedVersion(version.id(), version.label(), version.publishedAt()))
                    .toList();

            CatalogStudioSessionComposer composer = null;

            if (knownRevision > 0 && knownRevision <= revision) {
                composer = this.deltaComposer(state, knownRevision, revision, versions);
            }

            if (composer == null) {
                composer = this.fullComposer(state, revision, versions);
            }

            var response = composer.compose();
            if (this.client.getChannel().isActive()) this.client.sendResponse(response);
        } catch (RuntimeException exception) {
            LOGGER.error("[CatalogStudio] session open FAILED", exception);
        }
    }

    private CatalogStudioSessionComposer fullComposer(
            CatalogStudioSessionState state, long revision, List<CatalogStudioPublishedVersion> versions) {
        CachedSnapshot cached = CACHE.get();

        if (cached == null || cached.revision() != revision) {
            long started = System.currentTimeMillis();
            CatalogVersionSnapshot live = studio().liveMutations().loadLive();
            List<String> chunks = CatalogStudioSessionComposer.encodeSnapshot(live.pages(), live.offers());
            cached = new CachedSnapshot(live.version().revision(), chunks, live.pages().size(), live.offers().size());
            CACHE.set(cached);
            LOGGER.info(
                    "[CatalogStudio] full snapshot encoded for revision {} ({} pages, {} offers, {} chunks) in {} ms",
                    cached.revision(),
                    cached.pageCount(),
                    cached.offerCount(),
                    chunks.size(),
                    System.currentTimeMillis() - started);
            revision = cached.revision();
        }

        return new CatalogStudioSessionComposer(
                state.activeVersionId(),
                state.draftVersionId(),
                revision,
                state.activeUpdatedAt(),
                state.draftCreatedAt(),
                0,
                List.of(),
                true,
                0,
                versions,
                CatalogStudioSessionComposer.SNAPSHOT_ENCODING,
                cached.chunks(),
                cached.pageCount(),
                cached.offerCount());
    }

    /** Delta since {@code knownRevision}, or null when the journal cannot describe it cheaply. */
    private CatalogStudioSessionComposer deltaComposer(
            CatalogStudioSessionState state, long knownRevision, long revision, List<CatalogStudioPublishedVersion> versions) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            Map<CatalogPageType, Set<Integer>> touchedOffers = new EnumMap<>(CatalogPageType.class);
            Map<String, DeletedOffer> deleted = new LinkedHashMap<>();
            boolean pagesTouched = false;
            int touchedCount = 0;

            if (knownRevision < revision) {
                Gson gson = new Gson();
                try (PreparedStatement statement = connection.prepareStatement(CHANGES_SINCE_SQL)) {
                    statement.setLong(1, knownRevision);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            CatalogChangeEntry[] entries = gson.fromJson(resultSet.getString("changes_json"), CatalogChangeEntry[].class);
                            if (entries == null) continue;
                            for (CatalogChangeEntry entry : Arrays.asList(entries)) {
                                if (entry.entityType() == CatalogEntityType.PAGE) {
                                    pagesTouched = true;
                                    continue;
                                }
                                String key = entry.catalogType() + ":" + entry.entityId();
                                if (entry.operation() == CatalogChangeOperation.DELETE) {
                                    deleted.put(key, new DeletedOffer(entry.catalogType().name(), entry.entityId()));
                                } else {
                                    deleted.remove(key);
                                }
                                if (touchedOffers.computeIfAbsent(entry.catalogType(), ignored -> new LinkedHashSet<>()).add(entry.entityId())) {
                                    touchedCount++;
                                }
                                if (touchedCount > MAX_DELTA_OFFERS) return null;
                            }
                        }
                    }
                }
            }

            JdbcCatalogLiveSnapshotRepository snapshots = new JdbcCatalogLiveSnapshotRepository();
            List<CatalogPageSnapshot> pages = (pagesTouched || knownRevision < revision) ? snapshots.loadAllPages(connection) : List.of();
            List<CatalogOfferSnapshot> offers = new ArrayList<>();
            for (Map.Entry<CatalogPageType, Set<Integer>> touched : touchedOffers.entrySet()) {
                offers.addAll(snapshots.loadOffersByIds(connection, touched.getKey(), touched.getValue()));
            }

            // Anything touched but no longer present is a deletion, whatever the journal said last.
            Set<String> present = new LinkedHashSet<>();
            for (CatalogOfferSnapshot offer : offers) present.add(offer.catalogType() + ":" + offer.offerId());
            for (Map.Entry<CatalogPageType, Set<Integer>> touched : touchedOffers.entrySet()) {
                for (int offerId : touched.getValue()) {
                    String key = touched.getKey() + ":" + offerId;
                    if (!present.contains(key)) deleted.put(key, new DeletedOffer(touched.getKey().name(), offerId));
                }
            }

            DeltaPayload payload = new DeltaPayload(knownRevision, pages, offers, new ArrayList<>(deleted.values()));
            List<String> chunks = CatalogStudioSessionComposer.encodeJson(new Gson().toJson(payload));

            LOGGER.info(
                    "[CatalogStudio] delta {} -> {}: {} pages, {} offers, {} deleted",
                    knownRevision,
                    revision,
                    pages.size(),
                    offers.size(),
                    payload.deletedOffers().size());

            return new CatalogStudioSessionComposer(
                    state.activeVersionId(),
                    state.draftVersionId(),
                    revision,
                    state.activeUpdatedAt(),
                    state.draftCreatedAt(),
                    0,
                    List.of(),
                    true,
                    0,
                    versions,
                    CatalogStudioSessionComposer.DELTA_ENCODING,
                    chunks,
                    pages.size(),
                    offers.size());
        } catch (SQLException | RuntimeException exception) {
            LOGGER.warn("[CatalogStudio] delta since revision {} unavailable, sending the full snapshot", knownRevision, exception);
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static Instant now() {
        return Instant.now();
    }
}
