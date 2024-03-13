package com.eu.habbo.messages.outgoing.catalog.marketplace;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class MarketplaceBuyErrorComposer extends MessageComposer {
    public static final int REFRESH = 1;
    public static final int SOLD_OUT = 2;
    public static final int UPDATES = 3;
    public static final int NOT_ENOUGH_CREDITS = 4;

    private final int errorCode;
    private final int unknown;
    private final int offerId;
    private final int price;

    public MarketplaceBuyErrorComposer(int errorCode, int unknown, int offerId, int price) {
        this.errorCode = errorCode;
        this.unknown = unknown;
        this.offerId = offerId;
        this.price = price;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.MarketplaceBuyErrorComposer);
        this.response.appendInt(this.errorCode); //result
        this.response.appendInt(this.unknown); //newOfferId
        this.response.appendInt(this.offerId); //newPrice
        this.response.appendInt(this.price); //requestedOfferId
        return this.response;
    }
}
