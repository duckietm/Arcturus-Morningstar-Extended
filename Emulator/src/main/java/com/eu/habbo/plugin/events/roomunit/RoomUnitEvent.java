package com.eu.habbo.plugin.events.roomunit;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.plugin.Event;

public abstract class RoomUnitEvent extends Event {

    public final Room room;


    public final RoomUnit roomUnit;


    public RoomUnitEvent(Room room, RoomUnit roomUnit) {
        this.room = room;
        this.roomUnit = roomUnit;
    }
}
