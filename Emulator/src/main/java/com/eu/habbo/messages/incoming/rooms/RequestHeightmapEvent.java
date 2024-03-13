package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RequestHeightmapEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboInfo().getLoadingRoom() > 0) {
            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.client.getHabbo().getHabboInfo().getLoadingRoom());

            if (room != null) {
                Emulator.getGameEnvironment().getRoomManager().enterRoom(this.client.getHabbo(), room);

            }
        }
    }
}
