package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** CancelDailyQuest (3133): the daily widget cancels the tracked daily quest. */
public class CancelDailyQuestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Emulator.getGameEnvironment().getQuestManager().cancelDaily(this.client.getHabbo());
    }
}
