package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;

public class BotShoutEvent extends BotChatEvent {

    public BotShoutEvent(Bot bot, String message) {
        super(bot, message);
    }
}
