package com.eu.habbo.plugin.events.rooms;

import com.eu.habbo.habbohotel.rooms.Room;

public class RoomUnloadedEvent extends RoomEvent {

    public RoomUnloadedEvent(Room room) {
        super(room);
    }
}