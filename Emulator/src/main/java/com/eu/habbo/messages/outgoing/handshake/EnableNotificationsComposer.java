package com.eu.habbo.messages.outgoing.handshake;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class EnableNotificationsComposer extends MessageComposer {
    private final boolean enabled;

    public EnableNotificationsComposer(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.EnableNotificationsComposer);
        this.response.appendBoolean(this.enabled);
        return this.response;
    }
}
