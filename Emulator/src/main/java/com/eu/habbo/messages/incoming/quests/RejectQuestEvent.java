package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** RejectQuest (2397): stop tracking a quest. */
public class RejectQuestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int questId = this.packet.readInt();
        Emulator.getGameEnvironment().getQuestManager().cancel(this.client.getHabbo(), questId);
    }
}
