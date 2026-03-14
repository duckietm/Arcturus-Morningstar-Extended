package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public abstract class UserFriendEvent extends UserEvent {
    public final MessengerBuddy friend;


    public UserFriendEvent(Habbo habbo, MessengerBuddy friend) {
        super(habbo);

        this.friend = friend;
    }
}
