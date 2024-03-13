package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;

public class BotTalkEvent extends BotChatEvent {

    public BotTalkEvent(Bot bot, String message) {
        super(bot, message);
    }
}
