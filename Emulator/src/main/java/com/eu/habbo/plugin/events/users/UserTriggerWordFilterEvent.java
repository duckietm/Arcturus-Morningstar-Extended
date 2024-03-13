package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.modtool.WordFilterWord;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserTriggerWordFilterEvent extends UserEvent {

    public final WordFilterWord word;


    public UserTriggerWordFilterEvent(Habbo habbo, WordFilterWord word) {
        super(habbo);

        this.word = word;
    }
}
