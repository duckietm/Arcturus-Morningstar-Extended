package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RoomRemoveRightsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int roomId = this.packet.readInt();

        Emulator.getGameEnvironment().getRoomManager().getRoom(roomId).removeRights(this.client.getHabbo().getHabboInfo().getId());
    }
}