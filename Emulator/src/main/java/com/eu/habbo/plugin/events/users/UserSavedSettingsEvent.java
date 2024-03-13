package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserSavedSettingsEvent extends UserEvent {

    public UserSavedSettingsEvent(Habbo habbo) {
        super(habbo);
    }
}
