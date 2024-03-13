package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomCategoryUpdateMessageComposer extends MessageComposer {
    private final int unknownInt1;

    public RoomCategoryUpdateMessageComposer(int unknownInt1) {
        this.unknownInt1 = unknownInt1;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomCategoryUpdateMessageComposer);
        this.response.appendInt(this.unknownInt1);
        return this.response;
    }
}