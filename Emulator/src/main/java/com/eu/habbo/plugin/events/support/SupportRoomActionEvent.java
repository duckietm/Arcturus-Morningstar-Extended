package com.eu.habbo.plugin.events.support;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

public class SupportRoomActionEvent extends SupportEvent {

    public final Room room;


    public boolean kickUsers;


    public boolean lockDoor;


    public boolean changeTitle;


    public SupportRoomActionEvent(Habbo moderator, Room room, boolean kickUsers, boolean lockDoor, boolean changeTitle) {
        super(moderator);

        this.room = room;
        this.kickUsers = kickUsers;
        this.lockDoor = lockDoor;
        this.changeTitle = changeTitle;
    }
}