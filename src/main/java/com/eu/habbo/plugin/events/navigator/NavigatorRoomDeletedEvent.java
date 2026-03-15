package com.eu.habbo.plugin.events.navigator;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class NavigatorRoomDeletedEvent extends UserEvent {
    public final Room room;

    public NavigatorRoomDeletedEvent(Habbo habbo, Room room) {
        super(habbo);

        this.room = room;
    }
}
