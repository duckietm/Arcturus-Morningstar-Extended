package com.eu.habbo.messages.outgoing.catalog;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.catalog.CatalogPage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;

class CatalogPagesListComposerTest {
    @Test
    void publishesRealOfferIdsForExactPageNavigation() {
        CatalogPage page = mock(CatalogPage.class);
        when(page.getOfferIds()).thenReturn(new IntArrayList(new int[] { 2000029947, 2000029948 }));

        assertArrayEquals(
                new int[] { 2000029947, 2000029948 },
                CatalogPagesListComposer.navigationOfferIds(page));
    }
}
