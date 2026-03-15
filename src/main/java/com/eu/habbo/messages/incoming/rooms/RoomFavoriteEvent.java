package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.FavoriteRoomChangedComposer;

public class RoomFavoriteEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int roomId = this.packet.readInt();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);

        boolean added = true;
        if (room != null) {
            if (this.client.getHabbo().getHabboStats().hasFavoriteRoom(roomId)) {
                this.client.getHabbo().getHabboStats().removeFavoriteRoom(roomId);
                added = false;
            } else {
                if (!this.client.getHabbo().getHabboStats().addFavoriteRoom(roomId)) {
                    return;
                }
            }

            this.client.sendResponse(new FavoriteRoomChangedComposer(roomId, added));
        }
    }
}