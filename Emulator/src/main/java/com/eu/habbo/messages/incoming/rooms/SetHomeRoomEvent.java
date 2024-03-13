package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.UserHomeRoomComposer;

public class SetHomeRoomEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int roomId = this.packet.readInt();

        if (roomId != this.client.getHabbo().getHabboInfo().getHomeRoom()) {
            this.client.getHabbo().getHabboInfo().setHomeRoom(roomId);
            this.client.sendResponse(new UserHomeRoomComposer(this.client.getHabbo().getHabboInfo().getHomeRoom(), 0));
        }
    }
}
