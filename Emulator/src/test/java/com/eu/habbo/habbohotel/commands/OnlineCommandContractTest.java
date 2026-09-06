package com.eu.habbo.habbohotel.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OnlineCommandContractTest {
    @Test
    void onlineHasItsOwnPublicCommandAndListsLiveUsers() throws Exception {
        String about = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/AboutCommand.java"));
        String online = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/OnlineCommand.java"));
        String handler = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/CommandHandler.java"));

        assertFalse(about.contains("\"online\""));
        assertTrue(online.contains("super(\"cmd_online\", new String[] {\"online\""));
        assertTrue(online.contains("getOnlineHabbos()"));
        assertTrue(online.contains("String.CASE_INSENSITIVE_ORDER"));
        assertTrue(online.contains("getCurrentRoom()"));
        assertTrue(online.contains("getRank().getName()"));
        assertTrue(online.contains("room.getName()"));
        assertTrue(online.contains("room.getId()"));
        assertTrue(online.contains("Hotel View"));
        assertTrue(online.contains("gameClient.getHabbo().alert"));
        assertTrue(handler.contains("addCommand(new OnlineCommand())"));
    }
}
