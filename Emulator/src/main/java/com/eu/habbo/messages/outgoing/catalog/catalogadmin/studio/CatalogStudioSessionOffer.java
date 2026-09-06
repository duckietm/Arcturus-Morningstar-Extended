package com.eu.habbo.messages.outgoing.catalog.catalogadmin.studio;

import com.eu.habbo.habbohotel.catalog.versioning.CatalogOfferSnapshot;
import com.eu.habbo.habbohotel.catalog.versioning.CatalogPreviewProduct;
import java.util.List;
import java.util.Objects;

/** Complete editor projection for one draft offer. */
public record CatalogStudioSessionOffer(
        CatalogOfferSnapshot offer, List<CatalogPreviewProduct> products, boolean giftable) {
    public CatalogStudioSessionOffer {
        offer = Objects.requireNonNull(offer, "offer");
        products = List.copyOf(products);
    }
}
