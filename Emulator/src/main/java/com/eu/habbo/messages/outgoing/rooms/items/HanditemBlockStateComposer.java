package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomHanditemBlockSupport;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HanditemBlockStateComposer extends MessageComposer {
    private final int roomId;
    private final boolean blocked;

    public HanditemBlockStateComposer(Room room) {
        this.roomId = (room != null) ? room.getId() : 0;
        this.blocked = RoomHanditemBlockSupport.isHanditemBlocked(room);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HanditemBlockStateComposer);
        this.response.appendInt(this.roomId);
        this.response.appendBoolean(this.blocked);
        return this.response;
    }
}
