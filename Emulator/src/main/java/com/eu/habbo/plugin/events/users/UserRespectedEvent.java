package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserRespectedEvent extends UserEvent {
    public final Habbo from;

    public UserRespectedEvent(Habbo habbo, Habbo from) {
        super(habbo);

        this.from = from;
    }
}
