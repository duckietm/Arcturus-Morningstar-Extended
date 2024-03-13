package com.eu.habbo.plugin.events.rooms;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.plugin.Event;

public abstract class RoomEvent extends Event {

    public final Room room;


    public RoomEvent(Room room) {
        this.room = room;
    }
}