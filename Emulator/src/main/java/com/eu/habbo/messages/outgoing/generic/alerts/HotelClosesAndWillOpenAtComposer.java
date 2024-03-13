package com.eu.habbo.messages.outgoing.generic.alerts;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HotelClosesAndWillOpenAtComposer extends MessageComposer {
    private final int hour;
    private final int minute;
    private final boolean disconnected;

    public HotelClosesAndWillOpenAtComposer(int hour, int minute, boolean disconnected) {
        this.hour = hour;
        this.minute = minute;
        this.disconnected = disconnected;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HotelClosesAndWillOpenAtComposer);
        this.response.appendInt(this.hour);
        this.response.appendInt(this.minute);
        this.response.appendBoolean(this.disconnected);
        return this.response;
    }
}
