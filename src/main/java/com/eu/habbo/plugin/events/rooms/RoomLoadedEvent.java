package com.eu.habbo.plugin.events.rooms;

import com.eu.habbo.habbohotel.rooms.Room;

public class RoomLoadedEvent extends RoomEvent {

    public RoomLoadedEvent(Room room) {
        super(room);
    }
}