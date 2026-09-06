package com.eu.habbo.habbohotel.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RoomOwnerCommandsContractTest {

    @Test
    void ownerOnlyCommandsAndSocialCommandsAreRegistered() throws Exception {
        String source = read("CommandHandler.java");

        assertTrue(source.contains("new ClickInvisibleTilesCommand()"));
        assertTrue(source.contains("new KissCommand()"));
        assertTrue(source.contains("new PunchCommand()"));
        assertTrue(source.contains("new TogglePullPushCommand(true)"));
        assertTrue(source.contains("new TogglePullPushCommand(false)"));
        assertTrue(source.contains("new ToggleTradeCommand()"));
    }

    @Test
    void pullAndPushStayDisabledUntilTheRoomOwnerEnablesThem() throws Exception {
        String pull = read("PullCommand.java");
        String push = read("PushCommand.java");
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V20260810173000__room_social_commands_and_local_camera.sql"));

        assertTrue(pull.contains("!gameClient.getHabbo().getHabboInfo().getCurrentRoom().isPullEnabled()"));
        assertTrue(push.contains("!gameClient.getHabbo().getHabboInfo().getCurrentRoom().isPushEnabled()"));
        assertTrue(migration.contains("`pull_enabled` TINYINT(1) NOT NULL DEFAULT 0"));
        assertTrue(migration.contains("`push_enabled` TINYINT(1) NOT NULL DEFAULT 0"));
    }

    @Test
    void kissUsesTheHeartsBubbleForBothParticipants() throws Exception {
        String source = read("KissCommand.java");

        assertTrue(source.contains("commands.action.kiss.sender"));
        assertTrue(source.contains("commands.action.kiss.receiver"));
        assertTrue(source.indexOf("RoomChatMessageBubbles.HEARTS")
                != source.lastIndexOf("RoomChatMessageBubbles.HEARTS"));
    }

    @Test
    void cinvisibiliClicksFurnitureFromInvisibleCatalogCategoriesNotWiredTriggers() throws Exception {
        String source = read("ClickInvisibleTilesCommand.java");

        assertTrue(source.contains("getCatalogItems()"));
        assertTrue(source.contains("contains(\"invisibil\")"));
        assertTrue(source.contains("item.onClick(gameClient, room"));
        assertTrue(!source.contains("WiredManager"));
        assertTrue(!source.contains("room_invisible_click_tile"));
    }

    @Test
    void sharknadoSpawnsAndAnimatesTransientSharkFurniture() throws Exception {
        String source = read("TrashCommand.java");

        assertTrue(source.contains("js_r16_shark"));
        assertTrue(source.contains("seven_hammerheadshark"));
        assertTrue(source.contains("seven_shark"));
        assertTrue(source.contains("new AddFloorItemComposer"));
        assertTrue(source.contains("new FloorItemOnRollerComposer"));
        assertTrue(source.contains("new RemoveFloorItemComposer"));
        assertTrue(source.contains("ANIMATION_TICKS = 38"));
        assertTrue(source.contains("RAIN_CLOUD_EFFECT = 113"));
        assertTrue(source.contains("FLYING_EFFECT = 116"));
        assertTrue(source.contains("PIRATE_CREW_EFFECT = 161"));
        assertTrue(source.contains("restoreEffects"));
        assertTrue(source.contains("previous.effectId()"));
        assertTrue(!source.contains("addHabboItem"));
        assertTrue(!source.contains("createItem("));
        assertTrue(!source.contains("QueryDelete"));
    }

    @Test
    void tornadoCapturesTheCallerAndRestoresRealFurnitureWithoutPersistingVirtualPositions() throws Exception {
        String source = read("TrashCommand.java");

        assertTrue(source.contains("commandKey.equalsIgnoreCase(\"tornado\")"));
        assertTrue(source.contains("MAX_TORNADO_FURNI = 40"));
        assertTrue(source.contains("new RoomUnitOnRollerComposer"));
        assertTrue(source.contains("new FloorItemOnRollerComposer"));
        assertTrue(source.contains("new FloorItemUpdateComposer"));
        assertTrue(source.contains("unit.setCanWalk(false)"));
        assertTrue(source.contains("unit.setCanWalk(userState.couldWalk())"));
        assertTrue(!source.contains("room.updateItem(captured.item())"));
    }

    @Test
    void funCommandPackContainsAtLeastTwentyRoomAndTargetCommands() throws Exception {
        String source = read("FunRoomCommand.java");
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V20260810187000__fun_room_commands.sql"));

        for (String command : java.util.List.of(
                "disco", "rave", "terremoto", "cannoni", "levitazione", "girotondo", "invasionefufo",
                "piratiparty", "ghostparty", "robotparty", "pioggiadisco", "caos", "statue",
                "carnevale", "rapisci", "rimbalza", "vola", "orbita", "frullatore", "yoyo",
                "fantasma", "papera", "mummia", "zombie", "goblin", "alieno", "scambio",
                "calamita", "duello", "abbraccio", "telepatia", "catapulta", "spazio", "aurora",
                "temporale", "tramonto", "neonvoid", "blackout", "oceano", "inferno")) {
            assertTrue(migration.contains(command), "missing fun command: " + command);
        }
        assertTrue(source.contains("new AddFloorItemComposer"));
        assertTrue(source.contains("new RoomUnitOnRollerComposer"));
        assertTrue(source.contains("new FloorItemOnRollerComposer"));
        assertTrue(source.contains("restoreUser"));
        assertTrue(source.contains("restoreEarthquake"));
        assertTrue(source.contains("animatePairInteraction"));
        assertTrue(source.contains("finishPairInteraction"));
        assertTrue(source.contains("new InteractionBackgroundToner"));
        assertTrue(source.contains("new RoomPaintComposer"));
        assertTrue(source.contains("restoreAtmosphere"));
        assertTrue(source.contains("buildCannonLanes"));
        assertTrue(source.contains("room.getItemsAt(next)"));
        assertTrue(source.contains("impactProjectile"));
        assertTrue(source.contains("finishCannonBarrage"));
        assertTrue(source.contains("updateHomingDirection"));
        assertTrue(source.contains("homingTarget"));
    }

    @Test
    void destructiveFunEventsAreStaffOnlyFromSupportRankUpward() throws Exception {
        String source = read("FunRoomCommand.java");
        String trash = read("TrashCommand.java");
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V20260810188000__restrict_fun_commands_to_staff.sql"));

        assertTrue(source.contains("super(\"cmd_trash\""));
        assertTrue(trash.contains("super(\"cmd_trash\""));
        assertTrue(migration.contains("`rank_1` = 0"));
        assertTrue(migration.contains("`rank_3` = 0"));
        assertTrue(migration.contains("`rank_4` = 1"));
        assertTrue(migration.contains("`rank_7` = 1"));
        assertTrue(migration.contains("`level` >= 4"));
    }

    private static String read(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/eu/habbo/habbohotel/commands", file));
    }
}
