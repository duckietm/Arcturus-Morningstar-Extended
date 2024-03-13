package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UnknownAdManagerComposer extends MessageComposer {
    private final boolean unknownBoolean;

    public UnknownAdManagerComposer(boolean unknownBoolean) {
        this.unknownBoolean = unknownBoolean;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UnknownAdManagerComposer);
        this.response.appendBoolean(this.unknownBoolean);
        return this.response;
    }
}