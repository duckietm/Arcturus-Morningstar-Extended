package com.eu.habbo.messages.incoming.rooms.items;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RoomPickupTransferContractTest {
    @Test
    void ownerAndStaffKeepTheItemWhileRightsOnlyTransferItToTheRoomOwner() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/messages/incoming/rooms/items/RoomPickupItemEvent.java"));

        assertTrue(source.contains("Permission.ACC_ANYROOMOWNER"));
        assertTrue(source.contains("boolean keepsIt"));
        assertTrue(source.contains("item.setUserId(this.client.getHabbo().getHabboInfo().getId())"));
        assertTrue(source.contains("room.pickUpItem(item, this.client.getHabbo())"));
        assertTrue(source.contains("item.setUserId(room.getOwnerId())"));
        assertTrue(source.contains("room.ejectUserItem(item)"));
    }
}
