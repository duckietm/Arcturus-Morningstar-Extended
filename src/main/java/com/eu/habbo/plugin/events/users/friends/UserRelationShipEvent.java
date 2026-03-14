package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserRelationShipEvent extends UserFriendEvent {
    public int relationShip;


    public UserRelationShipEvent(Habbo habbo, MessengerBuddy friend, int relationShip) {
        super(habbo, friend);

        this.relationShip = relationShip;
    }
}