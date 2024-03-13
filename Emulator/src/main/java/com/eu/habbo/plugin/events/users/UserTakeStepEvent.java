package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserTakeStepEvent extends UserEvent {

    public final RoomTile fromLocation;


    public final RoomTile toLocation;


    public UserTakeStepEvent(Habbo habbo, RoomTile fromLocation, RoomTile toLocation) {
        super(habbo);

        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
    }
}
