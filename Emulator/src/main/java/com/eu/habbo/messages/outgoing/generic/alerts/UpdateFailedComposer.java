package com.eu.habbo.messages.outgoing.generic.alerts;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UpdateFailedComposer extends MessageComposer {
    private final String message;

    public UpdateFailedComposer(String message) {
        this.message = message;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UpdateFailedComposer);
        this.response.appendString(this.message);
        return this.response;
    }
}
