package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** ActivateQuest (793): accept a quest from the hotel view or the daily widget. */
public class ActivateQuestEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int questId = this.packet.readInt();
        Emulator.getGameEnvironment().getQuestManager().accept(this.client.getHabbo(), questId);
    }
}
