package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserFriendChatEvent extends UserFriendEvent {

    public String message;


    public UserFriendChatEvent(Habbo habbo, MessengerBuddy friend, String message) {
        super(habbo, friend);

        this.message = message;
    }
}
