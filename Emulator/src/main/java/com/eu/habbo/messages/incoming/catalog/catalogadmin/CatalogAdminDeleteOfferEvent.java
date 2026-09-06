package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeOperation;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogEntityType;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioMutationEnvelope;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRequestParser;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRuntime;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

public class CatalogAdminDeleteOfferEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CatalogAdminDeleteOfferEvent.class);

    @Override
    public void handle() throws Exception {
        try {
            this.handleGuarded();
        } catch (RuntimeException exception) {
            // A rejected edit must always answer the client: an escaped exception left the admin editor waiting
            // forever ("Saving page...") and blocked every later action until the client was reloaded.
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            LOGGER.warn("{} rejected: {}", this.getClass().getSimpleName(), message);
            this.client.sendResponse(new CatalogAdminResultComposer(false, message));
        }
    }

    private void handleGuarded() throws Exception {
        if (!this.client.getHabbo().hasPermission(Permission.ACC_CATALOGFURNI)) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "No permission"));
            return;
        }

        int offerId = this.packet.readInt();
        CatalogPageType pageType = CatalogPageType.fromString(this.packet.readString());

        if (offerId <= 0) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "Invalid offer id"));
            return;
        }

        CatalogStudioMutationEnvelope envelope = CatalogStudioRequestParser.parseMutationEnvelope(this.packet);
        int limitedSells = CatalogStudioRuntime.services()
                .operationalOffers()
                .findLimitedSells(offerId)
                .orElse(0);
        if (limitedSells > 0) {
            this.client.sendResponse(
                    new CatalogAdminResultComposer(false, "Limited offers with completed sales cannot be deleted"));
            return;
        }
        var result = CatalogStudioRuntime.services()
                .liveMutations()
                .apply(
                        CatalogAdminLiveRequest.of(
                                envelope,
                                this.client.getHabbo().getHabboInfo().getId(),
                                CatalogEntityType.OFFER,
                                pageType,
                                offerId,
                                CatalogChangeOperation.DELETE,
                                null),
                        live -> {
                            if (live.offer(pageType, offerId).isEmpty()) {
                                throw new IllegalArgumentException("Live catalog offer not found: " + offerId);
                            }
                        });
        this.client.sendResponse(
                new CatalogAdminResultComposer(true, "Offer deleted live at revision " + result.revision()));
    }
}
