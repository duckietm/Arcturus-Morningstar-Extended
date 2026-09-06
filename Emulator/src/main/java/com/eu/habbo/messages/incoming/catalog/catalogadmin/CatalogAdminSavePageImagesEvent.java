package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeOperation;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogSnapshotPatch;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioMutationEnvelope;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRequestParser;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRuntime;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;

public class CatalogAdminSavePageImagesEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CatalogAdminSavePageImagesEvent.class);

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
        String headerImage = this.packet.readString();
        String teaserImage = this.packet.readString();
        CatalogPageType pageType = CatalogPageType.fromString(this.packet.readString());

        CatalogStudioMutationEnvelope envelope = CatalogStudioRequestParser.parseMutationEnvelope(this.packet);
        var result = CatalogStudioRuntime.services()
                .liveMutations()
                .updatePage(
                        envelope.expectedRevision(),
                        envelope.operationId(),
                        this.client.getHabbo().getHabboInfo().getId(),
                        envelope.summary(),
                        pageType,
                        pageId,
                        page -> CatalogSnapshotPatch.setPageImages(page, headerImage, teaserImage),
                        CatalogChangeOperation.UPDATE);
        this.client.sendResponse(
                new CatalogAdminResultComposer(true, "Page images saved live at revision " + result.revision()));
    }
}
