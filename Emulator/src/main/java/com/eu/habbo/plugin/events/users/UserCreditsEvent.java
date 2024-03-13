package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserCreditsEvent extends UserEvent {

    public int credits;


    public UserCreditsEvent(Habbo habbo, int credits) {
        super(habbo);

        this.credits = credits;
    }
}