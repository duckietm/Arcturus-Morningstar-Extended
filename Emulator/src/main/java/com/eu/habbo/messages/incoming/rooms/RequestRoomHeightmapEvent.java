package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.RoomHeightMapComposer;
import com.eu.habbo.messages.outgoing.rooms.RoomRelativeMapComposer;

public class RequestRoomHeightmapEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboInfo().getLoadingRoom() > 0) {
            Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(this.client.getHabbo().getHabboInfo().getLoadingRoom());

            if (room != null && room.getLayout() != null) {
                this.client.sendResponse(new RoomRelativeMapComposer(room));

                this.client.sendResponse(new RoomHeightMapComposer(room));

                Emulator.getGameEnvironment().getRoomManager().enterRoom(this.client.getHabbo(), room);
            }
        }
    }
}
