package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserDisconnectEvent extends UserEvent {

    public UserDisconnectEvent(Habbo habbo) {
        super(habbo);
    }
}
