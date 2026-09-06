package com.eu.habbo.habbohotel.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.Item;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class BssCommandCompatibilityContractTest {
    @Test
    void everyBssAliasIsPresentInTheServerImplementation() throws Exception {
                String migration = Files.readString(Path.of(
                        "src/main/resources/db/migration/V20260810180000__bss_command_compatibility.sql"))
                + Files.readString(Path.of(
                        "src/main/resources/db/migration/V20260810190000__bss_give_prize_command.sql"))
                + Files.readString(
                        Path.of("src/main/resources/db/migration/V20260518000000__base_database.sql"))
                + Files.readString(Path.of(
                        "src/main/resources/db/migration/V20260810173000__room_social_commands_and_local_camera.sql"));
        String commands = Files.readString(
                        Path.of("src/main/java/com/eu/habbo/habbohotel/commands/CommandHandler.java"))
                + Files.readString(
                        Path.of("src/main/java/com/eu/habbo/habbohotel/commands/AboutCommand.java"))
                + Files.readString(Path.of(
                        "src/main/java/com/eu/habbo/habbohotel/commands/DisableMentionsCommand.java"));

        for (String alias : List.of(
                "pickall", "ejectall", "stenditi", "sit", "stand", "mutacuccioli", "mutabot",
                "copialook", "eff", "handitem", "mangia", "vai", "toglifaccia", "moonwalk",
                "scarica", "regenmaps", "pulisci", "maxutenti", "vroller", "diagonali", "stats",
                "cacciacuccioli", "cacciabot", "dnd", "bloccaregali", "convertcredits",
                "disablewhispers", "disablemimic", "pet", "teletrasporto", "coords", "chiudidadi",
                "ricarica", "informazioni", "apristanza", "chiudistanza", "disattivaeffetto",
                "ricaricami", "valore", "disablealert", "bacio", "hidewired", "placex",
                "forceheight", "forcerot", "disattivabacio", "rban", "cinvisibili",
                "puliscianimali", "puliscibots", "aggiungitag", "rimuovitag", "puliscitags",
                "togglepyramide", "report", "convertdiamonds", "scambia", "menzioni",
                "camminatacasuale", "eliminachatgruppo", "disattivachatgruppo", "tc", "resetprefix",
                "valori", "valoristanza", "giveprize")) {
            assertTrue(migration.contains(alias) || commands.contains(alias), "missing BSS alias: " + alias);
        }
    }

    @Test
    void givePrizeDrawsEveryRareRandomlyAndIsAdministratorOnly() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/BssGivePrizeCommand.java"));
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V20260810190000__bss_give_prize_command.sql"))
                + Files.readString(Path.of(
                        "src/main/resources/db/migration/V20260810191000__give_prize_staff_bubble.sql"));

        assertTrue(source.contains("while (result.size() < quantity)"));
        assertTrue(source.contains("Collections.shuffle(cycle, Emulator.getRandom())"));
        assertTrue(source.contains("cycle.remove(cycle.size() - 1)"));
        assertTrue(source.contains("getFurnitureValues().get(item.getSpriteId())"));
        assertTrue(source.contains("value[1] != requestedValue"));
        assertTrue(source.contains("createItem(connection, targetId, item, 0, 0, \"\")"));
        assertTrue(source.contains("target.addFurniture(created)"));
        assertTrue(source.contains("target.whisper"));
        assertTrue(source.contains("RoomChatMessageBubbles.STAFF"));
        assertTrue(!source.contains("target.alert"));
        assertTrue(migration.contains("'cmd_bss_give_prize', 1"));
        assertTrue(migration.contains("0, 0, 0, 0, 0, 0, 1"));
        assertTrue(migration.contains("Hai ricevuto un premio dallo staff: %items%"));
        assertTrue(migration.contains("Hai ricevuto %quantity% premi dallo staff"));
    }

    @Test
    void givePrizeDoesNotCloneOneRandomRareAcrossTheRequestedQuantity() {
        List<Item> candidates = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(spriteId -> {
                    Item item = mock(Item.class);
                    when(item.getSpriteId()).thenReturn(spriteId);
                    return item;
                })
                .toList();

        List<Item> selected = BssGivePrizeCommand.drawRandomRares(candidates, candidates.size());

        assertEquals(candidates.size(), selected.size());
        assertEquals(candidates.size(), new HashSet<>(selected).size());
    }

    @Test
    void placementOverridesHaveBatchAndPersistentModes() {
        int userId = 918273;
        BssPlacementPreferences.evict(userId);
        BssPlacementPreferences.setBatch(userId, 2, 4.25);
        BssPlacementPreferences.setForcedHeight(userId, 7.5);
        BssPlacementPreferences.setForcedRotation(userId, 6);

        assertTrue(BssPlacementPreferences.resolveRotation(userId, 2) == 6);
        assertTrue(BssPlacementPreferences.consumeHeight(userId) == 4.25);
        assertTrue(BssPlacementPreferences.consumeHeight(userId) == 4.25);
        assertTrue(BssPlacementPreferences.consumeHeight(userId) == 7.5);
        BssPlacementPreferences.evict(userId);
    }

    @Test
    void reportCommandAllowsStaffSelfTestsAndUsesVisibleTicketFeedback() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/eu/habbo/habbohotel/commands/BssSocialCommands.java"));
        String reportCommand = source.substring(
                source.indexOf("final class BssReportCommand"),
                source.indexOf("final class BssClearGroupChatCommand"));

        assertTrue(!reportCommand.contains("reported == gameClient.getHabbo()"));
        assertTrue(reportCommand.contains("new ModToolReportReceivedAlertComposer"));
        assertTrue(reportCommand.contains("new ReportRoomFormComposer"));
        assertTrue(!reportCommand.contains("new InsertModToolIssue"));
    }
}
