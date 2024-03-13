package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserRightsGivenEvent extends UserEvent {
    public final Habbo receiver;

    public UserRightsGivenEvent(Habbo habbo, Habbo receiver) {
        super(habbo);

        this.receiver = receiver;
    }
}
