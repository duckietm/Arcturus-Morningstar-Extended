package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.users.Habbo;

public class BotWhisperEvent extends BotChatEvent {

    public Habbo target;


    public BotWhisperEvent(Bot bot, String message, Habbo target) {
        super(bot, message);

        this.target = target;
    }
}
