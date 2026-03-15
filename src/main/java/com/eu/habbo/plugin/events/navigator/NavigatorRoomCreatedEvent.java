package com.eu.habbo.plugin.events.navigator;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class NavigatorRoomCreatedEvent extends UserEvent {

    public final Room room;


    public NavigatorRoomCreatedEvent(Habbo habbo, Room room) {
        super(habbo);

        this.room = room;
    }
}
