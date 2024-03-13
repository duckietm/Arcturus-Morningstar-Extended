package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;

public abstract class BotChatEvent extends BotEvent {

    public String message;


    public BotChatEvent(Bot bot, String message) {
        super(bot);

        this.message = message;
    }
}
