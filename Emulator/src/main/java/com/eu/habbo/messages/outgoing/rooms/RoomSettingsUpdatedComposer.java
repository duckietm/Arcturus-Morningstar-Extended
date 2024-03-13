package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomSettingsUpdatedComposer extends MessageComposer {
    private final Room room;

    public RoomSettingsUpdatedComposer(Room room) {
        this.room = room;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomSettingsUpdatedComposer);
        this.response.appendInt(this.room.getId());
        return this.response;
    }
}
