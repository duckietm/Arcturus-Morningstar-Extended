package com.eu.habbo.habbohotel.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SetHomeCommandContractTest {
    @Test
    void adminCanSetCurrentOrSpecifiedRoomAsEveryonesHome() throws Exception {
        String command = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/SetHomeCommand.java"));
        String handler = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/CommandHandler.java"));

        assertTrue(command.contains("super(\"cmd_set_home\""));
        assertTrue(command.contains("\"sethome\""));
        assertTrue(command.contains("getCurrentRoom()"));
        assertTrue(command.contains("Integer.parseInt(params[1])"));
        assertTrue(command.contains("HotelHomeRoomService.setForEveryone(room.getId())"));
        assertTrue(handler.contains("addCommand(new SetHomeCommand())"));
    }
}
