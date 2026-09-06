package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRuntime;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminOfferDetailsComposer;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

public class CatalogAdminLoadOfferEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CatalogAdminLoadOfferEvent.class);

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
        this.packet.readInt(); // legacy version field
        this.packet.readInt(); // legacy revision field
        var offer = CatalogStudioRuntime.services()
                .liveMutations()
                .loadLive()
                .offer(pageType, offerId)
                .orElse(null);
        if (offer == null) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "Live catalog offer not found: " + offerId));
            return;
        }
        int limitedSells = pageType == CatalogPageType.NORMAL
                ? CatalogStudioRuntime.services()
                        .operationalOffers()
                        .findLimitedSells(offerId)
                        .orElse(0)
                : 0;
        this.client.sendResponse(new CatalogAdminOfferDetailsComposer(offer, limitedSells));
    }
}
