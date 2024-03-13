package com.eu.habbo.messages.outgoing;

import com.eu.habbo.messages.ServerMessage;

public abstract class MessageComposer {

    private ServerMessage composed;
    protected final ServerMessage response;

    protected MessageComposer() {
        this.composed = null;
        this.response = new ServerMessage();
    }

    protected abstract ServerMessage composeInternal();

    public ServerMessage compose() {
        if (this.composed == null) {
            this.composed = this.composeInternal();
        }

        return this.composed;
    }

}