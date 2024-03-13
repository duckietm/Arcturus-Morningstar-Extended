package com.eu.habbo.plugin.events.rooms;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.Event;

public class UserVoteRoomEvent extends Event {
    public final Room room;
    public final Habbo habbo;

    public UserVoteRoomEvent(Room room, Habbo habbo) {
        this.room = room;
        this.habbo = habbo;
    }
}
