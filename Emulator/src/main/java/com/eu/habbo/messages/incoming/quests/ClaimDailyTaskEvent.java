package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** ClaimDailyTask (4101): claim the reward of a completed daily task. */
public class ClaimDailyTaskEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int taskId = this.packet.readInt();
        Emulator.getGameEnvironment().getDailyTaskManager().claim(this.client.getHabbo(), taskId);
    }
}
