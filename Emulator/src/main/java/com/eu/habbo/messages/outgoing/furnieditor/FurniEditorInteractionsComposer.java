package com.eu.habbo.messages.outgoing.furnieditor;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class FurniEditorInteractionsComposer extends MessageComposer {
    private final List<String> interactions;

    public FurniEditorInteractionsComposer(List<String> interactions) {
        this.interactions = interactions;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FurniEditorInteractionsComposer);
        this.response.appendInt(this.interactions.size());

        for (String interaction : this.interactions) {
            this.response.appendString(interaction);
        }

        return this.response;
    }
}
