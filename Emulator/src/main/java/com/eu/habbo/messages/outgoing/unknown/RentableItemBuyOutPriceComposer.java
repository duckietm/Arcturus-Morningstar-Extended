package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RentableItemBuyOutPriceComposer extends MessageComposer {
    private final boolean unknownBoolean1;
    private final String unknownString1;
    private final boolean unknownBoolean2;
    private final int credits;
    private final int points;
    private final int pointsType;

    public RentableItemBuyOutPriceComposer(boolean unknownBoolean1, String unknownString1, boolean unknownBoolean2, int credits, int points, int pointsType) {
        this.unknownBoolean1 = unknownBoolean1;
        this.unknownString1 = unknownString1;
        this.unknownBoolean2 = unknownBoolean2;
        this.credits = credits;
        this.points = points;
        this.pointsType = pointsType;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RentableItemBuyOutPriceComposer);
        this.response.appendBoolean(this.unknownBoolean1);
        this.response.appendString(this.unknownString1);
        this.response.appendBoolean(this.unknownBoolean2);
        this.response.appendInt(this.credits);
        this.response.appendInt(this.points);
        this.response.appendInt(this.pointsType);
        return this.response;
    }
}