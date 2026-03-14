package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserRegisteredEvent extends UserEvent {

    public UserRegisteredEvent(Habbo habbo) {
        super(habbo);
    }
}
