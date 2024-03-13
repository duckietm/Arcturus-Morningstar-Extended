package com.eu.habbo.messages.incoming.catalog.marketplace;

import com.eu.habbo.habbohotel.catalog.marketplace.MarketPlace;
import com.eu.habbo.habbohotel.catalog.marketplace.MarketPlaceOffer;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.marketplace.MarketplaceOffersComposer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestOffersEvent extends MessageHandler {
    public final static Map<Integer, ServerMessage> cachedResults = new ConcurrentHashMap<>(0);

    @Override
    public void handle() throws Exception {
        int min = this.packet.readInt();
        int max = this.packet.readInt();
        String query = this.packet.readString();
        int type = this.packet.readInt();

        boolean tryCache = false;
        if (min == -1 && max == -1 && query.isEmpty()) {
            tryCache = true;
        }

        if (tryCache) {
            ServerMessage message = cachedResults.get(type);
            if (message != null) {
                this.client.sendResponse(message);
                return;
            }
        }

        List<MarketPlaceOffer> offers = MarketPlace.getOffers(min, max, query, type);

        ServerMessage message = new MarketplaceOffersComposer(offers).compose();
        if (tryCache) {
            cachedResults.put(type, message);
        }

        this.client.sendResponse(message);
    }
}
