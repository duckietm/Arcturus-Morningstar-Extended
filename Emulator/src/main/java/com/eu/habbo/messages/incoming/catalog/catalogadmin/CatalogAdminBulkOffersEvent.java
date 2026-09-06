package com.eu.habbo.messages.incoming.catalog.catalogadmin;

// CATALOG_BULK_OFFERS_V1

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeOperation;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogEntityType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogLiveMutationRequest;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogOfferSnapshot;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioMutationEnvelope;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRequestParser;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRuntime;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CatalogAdminBulkOffersEvent extends MessageHandler {
    private static final int MAX_BATCH_SIZE = 500;
    private static final int MAX_JSON_LENGTH = 2_000_000;

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] No permission"));
            return;
        }

        String actionsJson = this.packet.readString();
        CatalogPageType pageType = CatalogPageType.fromString(this.packet.readString());
        CatalogStudioMutationEnvelope envelope = CatalogStudioRequestParser.parseMutationEnvelope(this.packet);
        Gson gson = new Gson();

        if (actionsJson == null || actionsJson.isBlank() || actionsJson.length() > MAX_JSON_LENGTH) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] Invalid payload"));
            return;
        }

        BulkAction[] rawActions;
        try {
            rawActions = gson.fromJson(actionsJson, BulkAction[].class);
        } catch (JsonSyntaxException exception) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] Invalid JSON"));
            return;
        }

        if (rawActions == null || rawActions.length == 0 || rawActions.length > MAX_BATCH_SIZE) {
            this.client.sendResponse(new CatalogAdminResultComposer(
                    false,
                    "[BULK_OFFERS] Batch must contain 1.." + MAX_BATCH_SIZE + " actions"));
            return;
        }

        List<PreparedAction> prepared = new ArrayList<>(rawActions.length);

        for (BulkAction action : rawActions) {
            if (action == null || action.action == null) {
                this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] Missing action"));
                return;
            }

            String kind = action.action.trim().toUpperCase(Locale.ROOT);

            if ("DELETE".equals(kind)) {
                if (action.offerId == null || action.offerId <= 0) {
                    this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] Invalid delete offer id"));
                    return;
                }

                prepared.add(new PreparedAction(kind, action.offerId, null));
                continue;
            }

            if (!"CREATE".equals(kind) && !"UPDATE".equals(kind)) {
                this.client.sendResponse(new CatalogAdminResultComposer(
                        false,
                        "[BULK_OFFERS] Unsupported action: " + kind));
                return;
            }

            if ("UPDATE".equals(kind) && (action.offerId == null || action.offerId <= 0)) {
                this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] Invalid update offer id"));
                return;
            }

            if (action.data == null) {
                this.client.sendResponse(new CatalogAdminResultComposer(false, "[BULK_OFFERS] Missing offer data"));
                return;
            }

            BulkOfferData data = action.data;

            CatalogAdminOfferPayload payload = CatalogAdminOfferPayload.validate(
                    data.pageId,
                    data.itemIds,
                    data.catalogName,
                    data.costCredits,
                    data.costPoints,
                    data.pointsType,
                    data.amount,
                    data.clubOnly,
                    data.extradata,
                    data.haveOffer,
                    data.offerIdGroup,
                    data.limitedStack,
                    data.orderNumber,
                    data.songId,
                    pageType);

            if (payload == null) {
                this.client.sendResponse(new CatalogAdminResultComposer(
                        false,
                        "[BULK_OFFERS] Invalid offer payload"));
                return;
            }

            for (int itemId : payload.baseItemIds()) {
                if (Emulator.getGameEnvironment().getItemManager().getItem(itemId) == null) {
                    this.client.sendResponse(new CatalogAdminResultComposer(
                            false,
                            "[BULK_OFFERS] Base item not found: " + itemId));
                    return;
                }
            }

            prepared.add(new PreparedAction(
                    kind,
                    "CREATE".equals(kind) ? 0 : action.offerId,
                    payload));
        }

        var liveMutations = CatalogStudioRuntime.services().liveMutations();
        var currentLive = liveMutations.loadLive();

        Map<String, CatalogOfferSnapshot> existingByItems = new LinkedHashMap<>();
        for (CatalogOfferSnapshot offer : currentLive.offers()) {
            if (offer.catalogType() == pageType) {
                existingByItems.putIfAbsent(normalizeItemIds(offer.itemIds()), offer);
            }
        }

        // A bulk import is idempotent. Repeated rows in the same payload are
        // collapsed (the last row wins), while an item already present anywhere
        // in this catalog is updated/moved instead of receiving another ID.
        Map<String, PreparedAction> createsByItems = new LinkedHashMap<>();
        List<PreparedAction> effectivePrepared = new ArrayList<>(prepared.size());
        for (PreparedAction action : prepared) {
            if (!"CREATE".equals(action.kind())) {
                effectivePrepared.add(action);
                continue;
            }

            String itemKey = normalizeItemIds(action.payload().itemIds);
            CatalogOfferSnapshot existing = existingByItems.get(itemKey);
            PreparedAction resolved = existing == null
                    ? action
                    : new PreparedAction("UPDATE", existing.offerId(), action.payload());

            if (createsByItems.containsKey(itemKey)) createsByItems.remove(itemKey);
            createsByItems.put(itemKey, resolved);
        }
        effectivePrepared.addAll(createsByItems.values());

        int actorId = this.client.getHabbo().getHabboInfo().getId();
        List<CatalogLiveMutationRequest> requests = new ArrayList<>(effectivePrepared.size());

        for (PreparedAction action : effectivePrepared) {
            switch (action.kind()) {
                case "CREATE" -> requests.add(CatalogAdminLiveRequest.of(
                        envelope,
                        actorId,
                        CatalogEntityType.OFFER,
                        pageType,
                        0,
                        CatalogChangeOperation.CREATE,
                        gson.toJson(CatalogAdminOfferDraftData.from(action.payload()))));

                case "UPDATE" -> requests.add(CatalogAdminLiveRequest.of(
                        envelope,
                        actorId,
                        CatalogEntityType.OFFER,
                        pageType,
                        action.offerId(),
                        CatalogChangeOperation.UPDATE,
                        gson.toJson(CatalogAdminOfferDraftData.from(action.payload()))));

                case "DELETE" -> requests.add(CatalogAdminLiveRequest.of(
                        envelope,
                        actorId,
                        CatalogEntityType.OFFER,
                        pageType,
                        action.offerId(),
                        CatalogChangeOperation.DELETE,
                        null));

                default -> throw new IllegalStateException("Unexpected bulk action " + action.kind());
            }
        }

        try {
            var result = liveMutations.applyBatch(
                    requests,
                    live -> {
                        for (PreparedAction action : effectivePrepared) {
                            if (action.payload() != null
                                    && live.page(pageType, action.payload().pageId).isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Live catalog page not found: " + action.payload().pageId);
                            }

                            if ("UPDATE".equals(action.kind())) {
                                CatalogOfferSnapshot existing =
                                        live.offer(pageType, action.offerId()).orElse(null);

                                if (existing == null) {
                                    throw new IllegalArgumentException(
                                            "Offer not found: " + action.offerId());
                                }

                                if (action.payload().limitedStack != 0
                                        && action.payload().limitedStack < existing.limitedStack()) {
                                    throw new IllegalArgumentException(
                                            "Limited stack cannot be reduced for offer " + action.offerId()
                                                    + " unless LTD is removed with 0");
                                }
                            }

                            if ("DELETE".equals(action.kind())
                                    && live.offer(pageType, action.offerId()).isEmpty()) {
                                throw new IllegalArgumentException(
                                        "Offer not found: " + action.offerId());
                            }
                        }
                    });

            this.client.sendResponse(new CatalogAdminResultComposer(
                    true,
                    "[BULK_OFFERS] Applied " + result.changes().size()
                            + " offer changes in one transaction at revision " + result.revision()));

        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();

            this.client.sendResponse(new CatalogAdminResultComposer(
                    false,
                    "[BULK_OFFERS] " + message));
        }
    }

    private static String normalizeItemIds(String itemIds) {
        if (itemIds == null || itemIds.isBlank()) return "";

        String[] raw = itemIds.split(",");
        List<Integer> ids = new ArrayList<>(raw.length);
        for (String value : raw) {
            try {
                ids.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException ignored) {
                return itemIds.trim().toLowerCase(Locale.ROOT);
            }
        }
        ids.sort(Integer::compareTo);
        return ids.stream().distinct().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private record PreparedAction(
            String kind,
            int offerId,
            CatalogAdminOfferPayload payload) {}

    private static final class BulkAction {
        String action;
        Integer offerId;
        BulkOfferData data;
    }

    private static final class BulkOfferData {
        int pageId;
        String itemIds = "";
        String catalogName = "";
        int costCredits;
        int costPoints;
        int pointsType;
        int amount = 1;
        int clubOnly;
        String extradata = "";
        boolean haveOffer = true;
        int offerIdGroup = -1;
        int limitedStack;
        int orderNumber;
        int songId;
    }
}
