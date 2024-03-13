package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserKickEvent extends UserEvent {

    public final Habbo target;


    public UserKickEvent(Habbo habbo, Habbo target) {
        super(habbo);

        this.target = target;
    }
}
