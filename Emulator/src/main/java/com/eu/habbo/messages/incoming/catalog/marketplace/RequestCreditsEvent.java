package com.eu.habbo.messages.incoming.catalog.marketplace;

import com.eu.habbo.habbohotel.catalog.marketplace.MarketPlace;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RequestCreditsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        MarketPlace.getCredits(this.client);
    }
}
