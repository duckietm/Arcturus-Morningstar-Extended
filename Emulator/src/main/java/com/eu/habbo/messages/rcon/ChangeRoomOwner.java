package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.google.gson.Gson;

public class ChangeRoomOwner extends RCONMessage<ChangeRoomOwner.JSON> {
    public ChangeRoomOwner() {
        super(JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(json.room_id);

        if (room != null) {
            room.setOwnerId(json.user_id);
            room.setOwnerName(json.username);
            room.setNeedsUpdate(true);
            room.save();
            Emulator.getGameEnvironment().getRoomManager().unloadRoom(room);
        }
    }

    static class JSON {

        public int room_id;


        public int user_id;


        public String username;
    }
}
