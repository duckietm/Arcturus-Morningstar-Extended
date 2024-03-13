package com.eu.habbo.messages.outgoing.handshake;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class PongComposer extends MessageComposer {
    private final int id;

    public PongComposer(int id) {
        this.id = id;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PongComposer);
        this.response.appendInt(this.id);
        return this.response;
    }
}
