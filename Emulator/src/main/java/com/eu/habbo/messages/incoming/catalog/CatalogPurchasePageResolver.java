package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.catalog.CatalogPageLayouts;
import java.util.Objects;
import java.util.function.Predicate;

final class CatalogPurchasePageResolver {

    interface CatalogLookup {
        CatalogItem findItem(int itemId);

        CatalogPage findPage(int pageId);
    }

    private final CatalogLookup catalog;

    CatalogPurchasePageResolver(CatalogManager catalogManager) {
        Objects.requireNonNull(catalogManager);
        this.catalog = new CatalogLookup() {
            @Override
            public CatalogItem findItem(int itemId) {
                return catalogManager.getCatalogItem(itemId);
            }

            @Override
            public CatalogPage findPage(int pageId) {
                return catalogManager.getCatalogPage(pageId);
            }
        };
    }

    CatalogPurchasePageResolver(CatalogLookup catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    CatalogPage resolve(CatalogPurchaseCommand command, Predicate<CatalogPage> canAccess) {
        CatalogItem canonicalItem = this.catalog.findItem(command.itemId());
        CatalogPage page;

        if (command.pageId() == -12345678 || command.pageId() == -1) {
            if (canonicalItem == null) {
                return null;
            }
            page = this.catalog.findPage(canonicalItem.getPageId());
        } else {
            page = this.catalog.findPage(command.pageId());

            // An infostand can retain a page id while its canonical offer is
            // being resolved. Never reject a real catalog item merely because
            // that stale page does not contain it: purchase from its actual,
            // access-checked page instead.
            if (canonicalItem != null && (page == null || page.getCatalogItem(command.itemId()) == null)) {
                page = this.catalog.findPage(canonicalItem.getPageId());
            }
        }

        if (page == null
                || page.getLayout() != null && page.getLayout().equalsIgnoreCase(CatalogPageLayouts.club_gift.name())) {
            return null;
        }
        return canAccess.test(page) ? page : null;
    }
}
