package com.eu.habbo.messages.outgoing.generic.alerts;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HotelClosedAndOpensComposer extends MessageComposer {
    private final int hour;
    private final int minute;

    public HotelClosedAndOpensComposer(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HotelClosedAndOpensComposer);
        this.response.appendInt(this.hour);
        this.response.appendInt(this.minute);
        return this.response;
    }
}
