package com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio;

import com.eu.habbo.habbohotel.catalog.versioning.CatalogOfferSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogPageSnapshot;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

/**
 * Catalog Studio session snapshot.
 *
 * V2 sends BOTH pages and offers in one compressed JSON payload and includes
 * authoritative counts before the chunks. The Nitro parser validates those
 * counts, so a partial/truncated snapshot cannot silently become an empty
 * offer list.
 */
public final class CatalogStudioSessionComposer extends MessageComposer {
    public static final String SNAPSHOT_ENCODING = "GZIP_BASE64_JSON_COUNTS_V2";
    /** Delta since a revision the client already holds: all pages, touched offers, deleted offer ids. */
    public static final String DELTA_ENCODING = "GZIP_BASE64_JSON_DELTA_V3";
    private static final int MAX_STRING_CHUNK_LENGTH = Short.MAX_VALUE;

    private final long activeVersionId;
    private final long draftVersionId;
    private final long revision;
    private final Instant activeUpdatedAt;
    private final Instant draftCreatedAt;
    private final int pendingCount;
    private final List<CatalogStudioActor> actors;
    private final boolean validationCurrent;
    private final int validationIssueCount;
    private final List<CatalogStudioPublishedVersion> publishedVersions;
    private final List<CatalogPageSnapshot> pages;
    private final List<CatalogOfferSnapshot> offers;
    private final String encoding;
    private final List<String> precomputedChunks;
    private final int precomputedPageCount;
    private final int precomputedOfferCount;

    private record SnapshotPayload(
            List<CatalogPageSnapshot> pages,
            List<CatalogOfferSnapshot> offers) {}

    public CatalogStudioSessionComposer(
            long activeVersionId,
            long draftVersionId,
            long revision,
            Instant activeUpdatedAt,
            Instant draftCreatedAt,
            int pendingCount,
            List<CatalogStudioActor> actors,
            boolean validationCurrent,
            int validationIssueCount,
            List<CatalogStudioPublishedVersion> publishedVersions) {
        this(
                activeVersionId,
                draftVersionId,
                revision,
                activeUpdatedAt,
                draftCreatedAt,
                pendingCount,
                actors,
                validationCurrent,
                validationIssueCount,
                publishedVersions,
                List.of(),
                List.of());
    }

    /**
     * Backward-compatible constructor used by older tests/callers.
     * It intentionally produces a zero-offer snapshot.
     */
    public CatalogStudioSessionComposer(
            long activeVersionId,
            long draftVersionId,
            long revision,
            Instant activeUpdatedAt,
            Instant draftCreatedAt,
            int pendingCount,
            List<CatalogStudioActor> actors,
            boolean validationCurrent,
            int validationIssueCount,
            List<CatalogStudioPublishedVersion> publishedVersions,
            List<CatalogPageSnapshot> pages) {
        this(
                activeVersionId,
                draftVersionId,
                revision,
                activeUpdatedAt,
                draftCreatedAt,
                pendingCount,
                actors,
                validationCurrent,
                validationIssueCount,
                publishedVersions,
                pages,
                List.of());
    }

    public CatalogStudioSessionComposer(
            long activeVersionId,
            long draftVersionId,
            long revision,
            Instant activeUpdatedAt,
            Instant draftCreatedAt,
            int pendingCount,
            List<CatalogStudioActor> actors,
            boolean validationCurrent,
            int validationIssueCount,
            List<CatalogStudioPublishedVersion> publishedVersions,
            List<CatalogPageSnapshot> pages,
            List<CatalogOfferSnapshot> offers) {
        this.activeVersionId = activeVersionId;
        this.draftVersionId = draftVersionId;
        this.revision = revision;
        this.activeUpdatedAt = Objects.requireNonNull(activeUpdatedAt, "activeUpdatedAt");
        this.draftCreatedAt = Objects.requireNonNull(draftCreatedAt, "draftCreatedAt");
        this.pendingCount = pendingCount;
        this.actors = List.copyOf(actors);
        this.validationCurrent = validationCurrent;
        this.validationIssueCount = validationIssueCount;
        this.publishedVersions = List.copyOf(publishedVersions);
        this.pages = List.copyOf(pages);
        this.offers = List.copyOf(offers);
        this.encoding = SNAPSHOT_ENCODING;
        this.precomputedChunks = null;
        this.precomputedPageCount = 0;
        this.precomputedOfferCount = 0;
    }

    /** Sends an already encoded payload (cached full snapshot or a delta) without re-serializing anything. */
    public CatalogStudioSessionComposer(
            long activeVersionId,
            long draftVersionId,
            long revision,
            Instant activeUpdatedAt,
            Instant draftCreatedAt,
            int pendingCount,
            List<CatalogStudioActor> actors,
            boolean validationCurrent,
            int validationIssueCount,
            List<CatalogStudioPublishedVersion> publishedVersions,
            String encoding,
            List<String> precomputedChunks,
            int pageCount,
            int offerCount) {
        this.activeVersionId = activeVersionId;
        this.draftVersionId = draftVersionId;
        this.revision = revision;
        this.activeUpdatedAt = Objects.requireNonNull(activeUpdatedAt, "activeUpdatedAt");
        this.draftCreatedAt = Objects.requireNonNull(draftCreatedAt, "draftCreatedAt");
        this.pendingCount = pendingCount;
        this.actors = List.copyOf(actors);
        this.validationCurrent = validationCurrent;
        this.validationIssueCount = validationIssueCount;
        this.publishedVersions = List.copyOf(publishedVersions);
        this.pages = List.of();
        this.offers = List.of();
        this.encoding = Objects.requireNonNull(encoding, "encoding");
        this.precomputedChunks = List.copyOf(precomputedChunks);
        this.precomputedPageCount = pageCount;
        this.precomputedOfferCount = offerCount;
    }

    public static List<String> encodeSnapshot(List<CatalogPageSnapshot> pages, List<CatalogOfferSnapshot> offers) {
        return encodeSnapshotChunks(new Gson().toJson(new SnapshotPayload(pages, offers)));
    }

    public static List<String> encodeJson(String json) {
        return encodeSnapshotChunks(json);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CatalogStudioSessionComposer);
        this.response.appendInt(Math.toIntExact(activeVersionId));
        this.response.appendInt(Math.toIntExact(draftVersionId));
        this.response.appendInt(Math.toIntExact(revision));
        this.response.appendString(activeUpdatedAt.toString());
        this.response.appendString(draftCreatedAt.toString());
        this.response.appendInt(pendingCount);
        this.response.appendInt(actors.size());

        for (CatalogStudioActor actor : actors) {
            this.response.appendInt(actor.id());
            this.response.appendString(actor.username());
        }

        this.response.appendBoolean(validationCurrent);
        this.response.appendInt(validationIssueCount);
        this.response.appendInt(publishedVersions.size());

        for (CatalogStudioPublishedVersion version : publishedVersions) {
            this.response.appendInt(Math.toIntExact(version.id()));
            this.response.appendString(version.label());
            this.response.appendString(version.publishedAt().toString());
        }

        List<String> chunks = this.precomputedChunks != null
                ? this.precomputedChunks
                : encodeSnapshotChunks(new Gson().toJson(new SnapshotPayload(pages, offers)));

        this.response.appendString(this.encoding);
        this.response.appendInt(this.precomputedChunks != null ? this.precomputedPageCount : pages.size());
        this.response.appendInt(this.precomputedChunks != null ? this.precomputedOfferCount : offers.size());
        this.response.appendInt(chunks.size());
        chunks.forEach(this.response::appendString);

        return this.response;
    }

    private static List<String> encodeSnapshotChunks(String snapshotJson) {
        byte[] json = snapshotJson.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(json);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to compress the Catalog Studio snapshot", exception);
        }

        String encoded = Base64.getEncoder().encodeToString(compressed.toByteArray());
        List<String> chunks = new ArrayList<>((encoded.length() / MAX_STRING_CHUNK_LENGTH) + 1);

        for (int offset = 0; offset < encoded.length(); offset += MAX_STRING_CHUNK_LENGTH) {
            chunks.add(encoded.substring(offset, Math.min(offset + MAX_STRING_CHUNK_LENGTH, encoded.length())));
        }

        return List.copyOf(chunks);
    }
}
