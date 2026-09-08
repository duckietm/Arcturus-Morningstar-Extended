package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** GetSeasonalQuestsOnly (1190): the seasonal calendar asks for the seasonal campaigns. */
public class GetSeasonalQuestsOnlyEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Emulator.getGameEnvironment().getQuestManager().sendSeasonalQuests(this.client.getHabbo());
    }
}
