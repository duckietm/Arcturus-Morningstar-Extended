package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ErrorLoginComposer extends MessageComposer {
    private final int errorCode;

    public ErrorLoginComposer(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ErrorLoginComposer);
        this.response.appendInt(this.errorCode);
        return this.response;
    }
}