package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class UserRequestFriendshipEvent extends UserEvent {

    public final String name;


    public final Habbo friend;


    public UserRequestFriendshipEvent(Habbo habbo, String name, Habbo friend) {
        super(habbo);

        this.name = name;
        this.friend = friend;
    }
}
