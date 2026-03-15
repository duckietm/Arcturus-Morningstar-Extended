package com.eu.habbo.plugin.events.furniture;

import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;

public class FurnitureRolledEvent extends FurnitureEvent {

    public final HabboItem roller;


    public final RoomTile newLocation;


    public FurnitureRolledEvent(HabboItem furniture, HabboItem roller, RoomTile newLocation) {
        super(furniture);

        this.roller = roller;
        this.newLocation = newLocation;
    }
}
