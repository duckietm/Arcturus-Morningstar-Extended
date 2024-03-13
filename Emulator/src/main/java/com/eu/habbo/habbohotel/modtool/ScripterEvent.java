package com.eu.habbo.habbohotel.modtool;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.support.SupportEvent;

public class ScripterEvent extends SupportEvent {
    public final Habbo habbo;
    public final String reason;

    public ScripterEvent(Habbo habbo, String reason) {
        super(null);

        this.habbo = habbo;
        this.reason = reason;
    }
}
