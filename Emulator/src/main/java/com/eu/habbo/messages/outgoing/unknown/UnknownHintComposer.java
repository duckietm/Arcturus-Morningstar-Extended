package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UnknownHintComposer extends MessageComposer {
    private final String key;

    public UnknownHintComposer(String key) {
        this.key = key;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UnknownHintComposer);
        this.response.appendString(this.key);
        return this.response;
    }
}