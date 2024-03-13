package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.habbohotel.catalog.TargetOffer;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.cache.HabboOfferPurchase;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class TargetedOfferComposer extends MessageComposer {
    private final Habbo habbo;
    private final TargetOffer offer;

    public TargetedOfferComposer(Habbo habbo, TargetOffer offer) {
        this.habbo = habbo;
        this.offer = offer;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.TargetedOfferComposer);
        HabboOfferPurchase purchase = HabboOfferPurchase.getOrCreate(this.habbo, this.offer.getId());
        this.offer.serialize(this.response, purchase);
        return this.response;
    }
}