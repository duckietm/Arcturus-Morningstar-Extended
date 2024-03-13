package com.eu.habbo.plugin.events.navigator;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;

public class NavigatorSearchResultEvent extends NavigatorRoomsEvent {

    public final String prefix;


    public final String query;


    public NavigatorSearchResultEvent(Habbo habbo, String prefix, String query, ArrayList<Room> rooms) {
        super(habbo, rooms);

        this.prefix = prefix;
        this.query = query;
    }
}
