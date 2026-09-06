package com.eu.habbo.messages.rcon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SetHomeRoomContractTest {
    @Test
    void canApplyTheImportedRoomToEveryExistingAndOnlineUser() throws Exception {
        String rcon = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/messages/rcon/SetHomeRoom.java"));
        String service = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/rooms/HotelHomeRoomService.java"));

        assertTrue(rcon.contains("HotelHomeRoomService.setForEveryone(json.room_id)"));
        assertTrue(service.contains("UPDATE users SET home_room = ?"));
        assertTrue(service.contains("hotel_home_room"));
        assertTrue(service.contains("getOnlineHabbos().values()"));
        assertTrue(service.contains("setHomeRoom(roomId)"));
        assertTrue(service.contains("RoomManager.HOME_ROOM_ID = roomId"));
    }
}
