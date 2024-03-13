package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UnknownComposer8 extends MessageComposer {
    private final int unknownInt1;
    private final int userId;
    private final int unknownInt2;

    public UnknownComposer8(int unknownInt1, int userId, int unknownInt2) {
        this.unknownInt1 = unknownInt1;
        this.userId = userId;
        this.unknownInt2 = unknownInt2;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UnknownComposer8);
        this.response.appendInt(this.unknownInt1);
        this.response.appendInt(this.userId);
        this.response.appendInt(this.unknownInt2);
        return this.response;
    }
}