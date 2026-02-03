package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RoomRemoveRightsEvent extends MessageHandler {

    @Override
    public int getRatelimit() {
        return 250;
    }

    @Override
    public void handle() throws Exception {
        final int roomId = this.packet.readInt();

        final Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);
        if (room == null) return;

        room.removeRights(this.client.getHabbo().getHabboInfo().getId());
    }
}