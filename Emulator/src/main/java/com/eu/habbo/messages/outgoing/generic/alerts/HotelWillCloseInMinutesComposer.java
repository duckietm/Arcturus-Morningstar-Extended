package com.eu.habbo.messages.outgoing.generic.alerts;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HotelWillCloseInMinutesComposer extends MessageComposer {
    private final int minutes;

    public HotelWillCloseInMinutesComposer(int minutes) {
        this.minutes = minutes;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HotelWillCloseInMinutesComposer);
        this.response.appendInt(this.minutes);
        return this.response;
    }
}
