package com.eu.habbo.messages.outgoing.rooms.promotions;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;
import java.util.List;

public class PromoteOwnRoomsListComposer extends MessageComposer {
    private final List<Room> rooms = new ArrayList<>();

    public PromoteOwnRoomsListComposer(List<Room> rooms) {
        for (Room room : rooms) {
            if (!room.isPromoted())
                this.rooms.add(room);
        }
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PromoteOwnRoomsListComposer);
        this.response.appendBoolean(true);
        this.response.appendInt(this.rooms.size());
        for (Room room : this.rooms) {
            this.response.appendInt(room.getId());
            this.response.appendString(room.getName());
            this.response.appendBoolean(true); //IDK what the fuck this is.
        }
        return this.response;
    }
}
