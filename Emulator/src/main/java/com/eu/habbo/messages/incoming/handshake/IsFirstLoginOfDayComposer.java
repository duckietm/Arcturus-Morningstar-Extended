package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class IsFirstLoginOfDayComposer extends MessageComposer {
    private final boolean isFirstLoginOfDay;

    public IsFirstLoginOfDayComposer(boolean isFirstLoginOfDay) {
        this.isFirstLoginOfDay = isFirstLoginOfDay;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.IsFirstLoginOfDayComposer);
        this.response.appendBoolean(this.isFirstLoginOfDay);
        return this.response;
    }
}