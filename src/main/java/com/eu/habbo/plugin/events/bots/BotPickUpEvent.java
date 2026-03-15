package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.users.Habbo;

public class BotPickUpEvent extends BotEvent {

    public final Habbo picker;


    public BotPickUpEvent(Bot bot, Habbo picker) {
        super(bot);

        this.picker = picker;
    }
}
