package com.eu.habbo.messages.outgoing.handshake;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ConnectionErrorComposer extends MessageComposer {
    private final int messageId;
    private final int errorCode;
    private final String timestamp;

    public ConnectionErrorComposer(int errorCode) {
        this.messageId = 0;
        this.errorCode = errorCode;
        this.timestamp = "";
    }

    public ConnectionErrorComposer(int messageId, int errorCode, String timestamp) {
        this.messageId = messageId;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ConnectionErrorComposer);
        this.response.appendInt(this.messageId);
        this.response.appendInt(this.errorCode);
        this.response.appendString(this.timestamp);

        return this.response;
    }
}
