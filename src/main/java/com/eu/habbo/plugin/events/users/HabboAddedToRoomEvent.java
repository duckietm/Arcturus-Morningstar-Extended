package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.Collection;

public class HabboAddedToRoomEvent extends UserEvent {

    public final Room room;
    public Collection<Habbo> habbosToSendEnter;
    public Collection<Habbo> visibleHabbos;

    public HabboAddedToRoomEvent(Habbo habbo, Room room, Collection<Habbo> habbosToSendEnter, Collection<Habbo> visibleHabbos) {
        super(habbo);

        this.room = room;
        this.habbosToSendEnter = habbosToSendEnter;
        this.visibleHabbos = visibleHabbos;
    }
}
