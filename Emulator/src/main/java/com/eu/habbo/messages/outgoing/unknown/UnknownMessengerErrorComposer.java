package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UnknownMessengerErrorComposer extends MessageComposer {
    private final int errorCode;
    private final int userId;
    private final String message;

    public UnknownMessengerErrorComposer(int errorCode, int userId, String message) {
        this.errorCode = errorCode;
        this.userId = userId;
        this.message = message;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UnknownMessengerErrorComposer);
        this.response.appendInt(this.errorCode);
        this.response.appendInt(this.userId);
        this.response.appendString(this.message);
        return this.response;
    }
}