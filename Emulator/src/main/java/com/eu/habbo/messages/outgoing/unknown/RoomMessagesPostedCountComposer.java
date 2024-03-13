package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomMessagesPostedCountComposer extends MessageComposer {
    private final Room room;
    private final int count;

    public RoomMessagesPostedCountComposer(Room room, int count) {
        this.room = room;
        this.count = count;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomMessagesPostedCountComposer);
        this.response.appendInt(this.room.getId());
        this.response.appendString(this.room.getName());
        this.response.appendInt(this.count);
        return this.response;
    }
}