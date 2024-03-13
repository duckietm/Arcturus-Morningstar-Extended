package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.users.Habbo;

public class BotServerItemEvent extends BotEvent {

    public Habbo habbo;

    public int itemId;


    public BotServerItemEvent(Bot bot, Habbo habbo, int itemId) {
        super(bot);

        this.habbo = habbo;
        this.itemId = itemId;
    }
}