package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.habbohotel.users.cache.HabboOfferPurchase;
import com.eu.habbo.messages.incoming.MessageHandler;

public class TargetOfferStateEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int id = this.packet.readInt();
        int state = this.packet.readInt();

        HabboOfferPurchase purchase = this.client.getHabbo().getHabboStats().getHabboOfferPurchase(id);
        if (purchase != null) {
            purchase.setState(state);
        }
    }
}