package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserAcceptFriendRequestEvent extends UserFriendEvent {

    public UserAcceptFriendRequestEvent(Habbo habbo, MessengerBuddy friend) {
        super(habbo, friend);
    }
}
