package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRuntime;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminPageDetailsComposer;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

public class CatalogAdminLoadPageEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CatalogAdminLoadPageEvent.class);

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

        int pageId = this.packet.readInt();
        CatalogPageType pageType = CatalogPageType.fromString(this.packet.readString());
        this.packet.readInt(); // legacy version field
        this.packet.readInt(); // legacy revision field
        var page = CatalogStudioRuntime.services()
                .liveMutations()
                .loadLive()
                .page(pageType, pageId)
                .orElse(null);
        if (page == null) {
            this.client.sendResponse(new CatalogAdminResultComposer(false, "Live catalog page not found: " + pageId));
            return;
        }

        this.client.sendResponse(new CatalogAdminPageDetailsComposer(page));
    }
}
