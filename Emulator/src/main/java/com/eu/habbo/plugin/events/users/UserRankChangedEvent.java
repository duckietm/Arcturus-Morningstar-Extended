package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserRankChangedEvent extends UserEvent {

    public UserRankChangedEvent(Habbo habbo) {
        super(habbo);
    }
}