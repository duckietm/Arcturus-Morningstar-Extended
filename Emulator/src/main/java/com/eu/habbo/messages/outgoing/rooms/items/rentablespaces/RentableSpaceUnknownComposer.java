package com.eu.habbo.messages.outgoing.rooms.items.rentablespaces;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RentableSpaceUnknownComposer extends MessageComposer {
    private final int itemId;

    public RentableSpaceUnknownComposer(int itemId) {
        this.itemId = itemId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RentableSpaceUnknownComposer);
        this.response.appendInt(this.itemId);
        return this.response;
    }
}
