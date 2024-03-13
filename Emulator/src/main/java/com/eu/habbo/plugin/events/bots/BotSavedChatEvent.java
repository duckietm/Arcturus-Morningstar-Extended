package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;

import java.util.ArrayList;

public class BotSavedChatEvent extends BotEvent {

    public boolean autoChat;


    public boolean randomChat;


    public int chatDelay;


    public ArrayList<String> chat;


    public BotSavedChatEvent(Bot bot, boolean autoChat, boolean randomChat, int chatDelay, ArrayList<String> chat) {
        super(bot);

        this.autoChat = autoChat;
        this.randomChat = randomChat;
        this.chatDelay = chatDelay;
        this.chat = chat;
    }
}
