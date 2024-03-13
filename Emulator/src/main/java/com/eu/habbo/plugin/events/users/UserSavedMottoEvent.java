package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserSavedMottoEvent extends UserEvent {
    public final String oldMotto;
    public String newMotto;


    public UserSavedMottoEvent(Habbo habbo, String oldMotto, String newMotto) {
        super(habbo);
        this.oldMotto = oldMotto;
        this.newMotto = newMotto;
    }
}
