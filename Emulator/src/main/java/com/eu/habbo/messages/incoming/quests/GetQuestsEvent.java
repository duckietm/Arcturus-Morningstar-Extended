package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** GetQuests (3333): the quests window asks for one entry per campaign. */
public class GetQuestsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Emulator.getGameEnvironment().getQuestManager().sendQuests(this.client.getHabbo(), true);
    }
}
