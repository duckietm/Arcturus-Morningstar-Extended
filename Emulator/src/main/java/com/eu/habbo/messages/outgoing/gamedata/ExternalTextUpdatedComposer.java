package com.eu.habbo.messages.outgoing.gamedata;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** CUSTOM packet 10091: result of an external text change; on success it is broadcast to every client. */
public class ExternalTextUpdatedComposer extends MessageComposer {
    private final boolean success;
    private final String key;
    private final String value;
    private final String message;

    public ExternalTextUpdatedComposer(boolean success, String key, String value, String message) {
        this.success = success;
        this.key = key == null ? "" : key;
        this.value = value == null ? "" : value;
        this.message = message == null ? "" : message;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ExternalTextUpdatedComposer);
        this.response.appendBoolean(this.success);
        this.response.appendString(this.key);
        this.response.appendString(this.value);
        this.response.appendString(this.message);
        return this.response;
    }
}
