package com.eu.habbo.plugin.events.sanctions;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.support.SupportEvent;

public class SanctionEvent extends SupportEvent {
    public Habbo target;

    public int sanctionLevel;

    public SanctionEvent(Habbo moderator, Habbo target, int sanctionLevel) {
        super(moderator);

        this.target = target;
        this.sanctionLevel = sanctionLevel;
    }
}
