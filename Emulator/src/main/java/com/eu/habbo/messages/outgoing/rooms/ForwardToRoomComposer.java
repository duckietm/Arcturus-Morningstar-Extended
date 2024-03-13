package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ForwardToRoomComposer extends MessageComposer {
    private final int roomId;

    public ForwardToRoomComposer(int roomId) {
        this.roomId = roomId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ForwardToRoomComposer);
        this.response.appendInt(this.roomId);
        return this.response;
    }
}
