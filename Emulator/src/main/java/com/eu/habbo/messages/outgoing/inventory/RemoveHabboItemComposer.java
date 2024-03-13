package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RemoveHabboItemComposer extends MessageComposer {
    private final int itemId;

    public RemoveHabboItemComposer(final int itemId) {
        this.itemId = itemId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RemoveHabboItemComposer);
        this.response.appendInt(this.itemId);
        return this.response;
    }
}
