package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** GetDailyQuest (2486): the daily widget asks for the index-th open quest of a difficulty. */
public class GetDailyQuestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        boolean easy = this.packet.readBoolean();
        int index = this.packet.readInt();
        Emulator.getGameEnvironment().getQuestManager().sendDailyQuest(this.client.getHabbo(), easy, index);
    }
}
