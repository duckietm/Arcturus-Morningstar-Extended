package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RoomVoteEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Emulator.getGameEnvironment().getRoomManager().voteForRoom(this.client.getHabbo(), this.client.getHabbo().getHabboInfo().getCurrentRoom());
    }
}
