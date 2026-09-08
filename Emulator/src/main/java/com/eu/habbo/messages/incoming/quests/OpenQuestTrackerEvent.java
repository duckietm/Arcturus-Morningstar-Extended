package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** OpenQuestTracker (2750): the tracker asks for the quest it should follow after a completion. */
public class OpenQuestTrackerEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Emulator.getGameEnvironment().getQuestManager().sendTrackedQuest(this.client.getHabbo());
    }
}
