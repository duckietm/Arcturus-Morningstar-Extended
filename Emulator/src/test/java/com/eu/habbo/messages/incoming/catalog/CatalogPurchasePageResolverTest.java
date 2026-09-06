package com.eu.habbo.messages.incoming.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.catalog.CatalogPageLayouts;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CatalogPurchasePageResolverTest {

    @Test
    void unknownSearchItemDoesNotEvaluatePageAccess() {
        AtomicBoolean accessEvaluated = new AtomicBoolean();
        CatalogPurchasePageResolver resolver = resolver(null, null);

        CatalogPage resolved = resolver.resolve(new CatalogPurchaseCommand(-1, 404, "", 1), page -> {
            accessEvaluated.set(true);
            return true;
        });

        assertNull(resolved);
        assertFalse(accessEvaluated.get());
    }

    @Test
    void searchResultDoesNotRequireAPositiveLegacyOfferId() {
        CatalogItem item = mock(CatalogItem.class);
        CatalogPage page = mock(CatalogPage.class);
        when(item.getPageId()).thenReturn(19);
        CatalogPurchasePageResolver.CatalogLookup lookup = mock(CatalogPurchasePageResolver.CatalogLookup.class);
        when(lookup.findItem(72)).thenReturn(item);
        when(lookup.findPage(19)).thenReturn(page);
        CatalogPurchasePageResolver resolver = new CatalogPurchasePageResolver(lookup);

        CatalogPage resolved =
                resolver.resolve(new CatalogPurchaseCommand(-1, 72, "", 1), candidate -> candidate == page);

        assertSame(page, resolved);
        verify(lookup).findPage(19);
        verify(item, never()).getOfferId();
    }

    @Test
    void directPageUsesThePageIdWithoutAnItemLookup() {
        CatalogPage page = mock(CatalogPage.class);
        CatalogPurchasePageResolver.CatalogLookup lookup = mock(CatalogPurchasePageResolver.CatalogLookup.class);
        when(lookup.findPage(11)).thenReturn(page);
        CatalogPurchasePageResolver resolver = new CatalogPurchasePageResolver(lookup);

        assertSame(page, resolver.resolve(new CatalogPurchaseCommand(11, 72, "", 1), candidate -> true));
    }

    @Test
    void staleVisualizerPageFallsBackToTheCanonicalOfferPage() {
        CatalogItem item = mock(CatalogItem.class);
        CatalogPage stalePage = mock(CatalogPage.class);
        CatalogPage canonicalPage = mock(CatalogPage.class);
        when(item.getPageId()).thenReturn(900102);

        CatalogPurchasePageResolver.CatalogLookup lookup = mock(CatalogPurchasePageResolver.CatalogLookup.class);
        when(lookup.findItem(2000029947)).thenReturn(item);
        when(lookup.findPage(17)).thenReturn(stalePage);
        when(lookup.findPage(900102)).thenReturn(canonicalPage);

        CatalogPurchasePageResolver resolver = new CatalogPurchasePageResolver(lookup);

        assertSame(
                canonicalPage,
                resolver.resolve(new CatalogPurchaseCommand(17, 2000029947, "", 1), candidate -> true));
    }

    @Test
    void directPageContainingTheOfferRemainsSelected() {
        CatalogItem item = mock(CatalogItem.class);
        CatalogPage requestedPage = mock(CatalogPage.class);
        when(requestedPage.getCatalogItem(72)).thenReturn(item);

        CatalogPurchasePageResolver.CatalogLookup lookup = mock(CatalogPurchasePageResolver.CatalogLookup.class);
        when(lookup.findItem(72)).thenReturn(item);
        when(lookup.findPage(11)).thenReturn(requestedPage);

        CatalogPurchasePageResolver resolver = new CatalogPurchasePageResolver(lookup);

        assertSame(requestedPage, resolver.resolve(new CatalogPurchaseCommand(11, 72, "", 1), candidate -> true));
        verify(item, never()).getPageId();
    }

    @Test
    void clubGiftPageIsNotPurchasableThroughThisCommand() {
        CatalogPage page = mock(CatalogPage.class);
        when(page.getLayout()).thenReturn(CatalogPageLayouts.club_gift.name());
        AtomicBoolean accessEvaluated = new AtomicBoolean();
        CatalogPurchasePageResolver resolver = resolver(null, page);

        assertNull(resolver.resolve(new CatalogPurchaseCommand(11, 72, "", 1), candidate -> {
            accessEvaluated.set(true);
            return true;
        }));
        assertFalse(accessEvaluated.get());
    }

    private static CatalogPurchasePageResolver resolver(CatalogItem item, CatalogPage page) {
        return new CatalogPurchasePageResolver(new CatalogPurchasePageResolver.CatalogLookup() {
            @Override
            public CatalogItem findItem(int itemId) {
                return item;
            }

            @Override
            public CatalogPage findPage(int pageId) {
                return page;
            }
        });
    }
}
