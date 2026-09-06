package com.eu.habbo.messages.incoming.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogSearchOfferIdContractTest {
    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test
    void catalogItemsUseTheUniqueSerializedCatalogItemIdForSearch() throws Exception {
        String source = source("src/main/java/com/eu/habbo/habbohotel/catalog/CatalogItem.java");

        int method = source.indexOf("public int getSearchOfferId()");
        int canonicalId = source.indexOf("return this.id", method);

        assertTrue(method > -1, "CatalogItem should expose a search-safe offer id");
        assertTrue(canonicalId > method,
                "Catalog search must use the unique id Nitro receives instead of the non-unique legacy offer_id");
        assertTrue(!source.substring(method, canonicalId).contains("this.offerId"),
                "Legacy offer_id must not select a different product during lazy search activation");
    }

    @Test
    void catalogManagerIndexesSearchOfferIdsInsteadOfRawOfferIds() throws Exception {
        String source = source("src/main/java/com/eu/habbo/habbohotel/catalog/CatalogManager.java");

        int searchOffer = source.indexOf("int searchOfferId = item.getSearchOfferId()");
        int addOffer = source.indexOf("page.addOfferId(searchOfferId)", searchOffer);
        int offerDefs = source.indexOf("this.offerDefs.put(searchOfferId, item.getId())", addOffer);

        assertTrue(searchOffer > -1, "CatalogManager should calculate the runtime search offer id");
        assertTrue(addOffer > searchOffer, "CatalogManager should expose runtime search offer ids in catalog pages");
        assertTrue(offerDefs > addOffer, "CatalogManager should map runtime search offer ids back to catalog items");
        assertTrue(!source.contains("this.offerDefs.put(item.getOfferId(), item.getId())"),
                "CatalogManager must not index raw -1 offer ids for catalog search");
    }

    @Test
    void catalogSearchLookupResolvesCatalogItemIdsAndComparesSearchOfferIds() throws Exception {
        String source = source("src/main/java/com/eu/habbo/messages/incoming/catalog/CatalogSearchedItemEvent.java");

        assertTrue(source.contains("int catalogItemId ="),
                "Catalog search lookup should name offerDefs values as catalog item ids");
        assertTrue(source.contains("getCatalogItem(catalogItemId)"),
                "Catalog search should resolve the mapped catalog item directly");
        assertTrue(source.contains("item.getSearchOfferId() == offerId"),
                "Catalog search should compare runtime search offer ids, not raw database offer ids");
    }
}
