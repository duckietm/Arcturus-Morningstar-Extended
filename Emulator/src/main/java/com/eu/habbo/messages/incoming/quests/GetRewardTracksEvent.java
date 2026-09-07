package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

/** GetRewardTracks (9450, custom): the client asks for the active reward tracks. */
public class GetRewardTracksEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Emulator.getGameEnvironment().getRewardTrackManager().sendRewardTracks(this.client.getHabbo(), false);
    }
}
