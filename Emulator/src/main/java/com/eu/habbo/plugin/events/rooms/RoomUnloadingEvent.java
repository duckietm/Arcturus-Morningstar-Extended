package com.eu.habbo.plugin.events.rooms;

import com.eu.habbo.habbohotel.rooms.Room;

public class RoomUnloadingEvent extends RoomEvent {

    public RoomUnloadingEvent(Room room) {
        super(room);
    }
}