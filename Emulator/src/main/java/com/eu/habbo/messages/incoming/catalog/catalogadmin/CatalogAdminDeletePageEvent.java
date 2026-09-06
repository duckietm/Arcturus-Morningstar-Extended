package com.eu.habbo.messages.incoming.catalog.catalogadmin;

import com.eu.habbo.habbohotel.catalog.CatalogPageType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogChangeOperation;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogEntityType;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogLiveMutationRequest;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogOfferSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogPageSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogVersionSnapshot;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioMutationEnvelope;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRequestParser;
import com.eu.habbo.messages.incoming.catalog.catalogadmin.studio.CatalogStudioRuntime;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Deletes a catalog page <em>and everything under it</em>: every offer on the page, every descendant page and their
 * offers, deepest first, in one live batch (one transaction, one history group, undoable as a unit). The old handler
 * refused a page that still had children or offers; the studio user decides, so the cascade is now the behaviour.
 */
public class CatalogAdminDeletePageEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CatalogAdminDeletePageEvent.class);

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
        CatalogStudioMutationEnvelope envelope = CatalogStudioRequestParser.parseMutationEnvelope(this.packet);
        int actorId = this.client.getHabbo().getHabboInfo().getId();

        var liveMutations = CatalogStudioRuntime.services().liveMutations();
        CatalogVersionSnapshot live = liveMutations.loadLive();
        if (live.page(pageType, pageId).isEmpty()) {
            throw new IllegalArgumentException("Live catalog page not found: " + pageId);
        }

        // Pages to remove, parents before children; reversed below so the deepest go first.
        List<Integer> pageIds = collectSubtree(live, pageType, pageId);
        List<Integer> offerIds = new ArrayList<>();
        for (CatalogOfferSnapshot offer : live.offers()) {
            if (offer.catalogType() == pageType && pageIds.contains(offer.pageId())) offerIds.add(offer.offerId());
        }

        List<CatalogLiveMutationRequest> requests = new ArrayList<>(offerIds.size() + pageIds.size());
        for (int offerId : offerIds) {
            requests.add(CatalogAdminLiveRequest.of(
                    envelope, actorId, CatalogEntityType.OFFER, pageType, offerId, CatalogChangeOperation.DELETE, null));
        }
        for (int index = pageIds.size() - 1; index >= 0; index--) {
            requests.add(CatalogAdminLiveRequest.of(
                    envelope, actorId, CatalogEntityType.PAGE, pageType, pageIds.get(index), CatalogChangeOperation.DELETE, null));
        }

        Set<Integer> plannedPages = new LinkedHashSet<>(pageIds);
        Set<Integer> plannedOffers = new LinkedHashSet<>(offerIds);

        var result = liveMutations.applyBatch(requests, current -> {
            // Somebody added a page or an offer under this subtree between our read and the locked apply:
            // refuse rather than leave orphans behind, the client simply retries with a fresh snapshot.
            for (CatalogPageSnapshot page : current.pages()) {
                if (page.catalogType() == pageType && plannedPages.contains(page.parentId()) && !plannedPages.contains(page.pageId())) {
                    throw new IllegalArgumentException("The page changed while deleting, retry");
                }
            }
            for (CatalogOfferSnapshot offer : current.offers()) {
                if (offer.catalogType() == pageType && plannedPages.contains(offer.pageId()) && !plannedOffers.contains(offer.offerId())) {
                    throw new IllegalArgumentException("The page changed while deleting, retry");
                }
            }
        });

        int childPages = pageIds.size() - 1;
        this.client.sendResponse(new CatalogAdminResultComposer(
                true,
                "Page deleted live at revision " + result.revision()
                        + " (" + childPages + " sub-pages, " + offerIds.size() + " offers)"));
    }

    /** The page and all its descendants, breadth first (a parent always precedes its children). */
    private static List<Integer> collectSubtree(CatalogVersionSnapshot live, CatalogPageType pageType, int rootId) {
        List<Integer> ordered = new ArrayList<>();
        ordered.add(rootId);

        for (int index = 0; index < ordered.size(); index++) {
            int parentId = ordered.get(index);
            for (CatalogPageSnapshot page : live.pages()) {
                if (page.catalogType() == pageType && page.parentId() == parentId && !ordered.contains(page.pageId())) {
                    ordered.add(page.pageId());
                }
            }
        }

        return ordered;
    }
}
