package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.marketplace.MarketplaceConfigComposer;

public class RequestMarketplaceConfigEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new MarketplaceConfigComposer());
    }
}
