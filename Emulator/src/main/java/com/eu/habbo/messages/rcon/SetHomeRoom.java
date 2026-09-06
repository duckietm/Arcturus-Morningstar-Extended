package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.HotelHomeRoomService;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.google.gson.Gson;
import jakarta.validation.constraints.Positive;
import java.sql.SQLException;

/** Updates the persisted and live hotel.home.room value together. */
public final class SetHomeRoom extends RCONMessage<SetHomeRoom.JSON> {
    public SetHomeRoom() {
        super(JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        RoomManager roomManager = Emulator.getGameEnvironment().getRoomManager();
        Room room = roomManager.getRoom(json.room_id);
        boolean loadedForRequest = false;
        if (room == null) {
            room = roomManager.loadRoom(json.room_id, false);
            loadedForRequest = room != null;
        }
        if (room == null) {
            this.status = ROOM_NOT_FOUND;
            this.message = "home room not found";
            return;
        }

        try {
            int updatedUsers = HotelHomeRoomService.setForEveryone(json.room_id);
            this.message = "updated hotel home room to " + json.room_id + " for " + updatedUsers + " users";
        } catch (SQLException exception) {
            this.status = SYSTEM_ERROR;
            this.message = "failed to update hotel home room";
        } finally {
            if (loadedForRequest) {
                roomManager.unloadRoom(room);
            }
        }
    }

    public static final class JSON {
        @Positive(message = "invalid room")
        public int room_id;

        public boolean all_users;
    }
}
