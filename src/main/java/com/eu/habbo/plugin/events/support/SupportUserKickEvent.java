package com.eu.habbo.plugin.events.support;

import com.eu.habbo.habbohotel.users.Habbo;

public class SupportUserKickEvent extends SupportEvent {

    public Habbo target;


    public String message;


    public SupportUserKickEvent(Habbo moderator, Habbo target, String message) {
        super(moderator);

        this.target = target;
        this.message = message;
    }
}