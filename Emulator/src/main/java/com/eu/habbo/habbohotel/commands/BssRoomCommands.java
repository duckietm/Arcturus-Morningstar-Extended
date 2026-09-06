package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.interactions.InteractionDice;
import com.eu.habbo.habbohotel.items.interactions.InteractionPyramid;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.rooms.RoomState;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.RoomSettingsUpdatedComposer;
import com.eu.habbo.messages.outgoing.users.UserCreditsComposer;
import java.util.ArrayList;
import java.util.List;

final class BssKickPetsCommand extends Command {
    BssKickPetsCommand() {
        super("cmd_bss_kick_pets", Emulator.getTexts().getValue("commands.keys.cmd_bss_kick_pets").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = BssRoomCommandSupport.ownerRoom(gameClient);
        if (room == null) return true;
        int count = room.getCurrentPets().size();
        room.removeAllPets();
        BssRoomCommandSupport.notifyCount(gameClient, permission, count);
        return true;
    }
}

final class BssKickBotsCommand extends Command {
    BssKickBotsCommand() {
        super("cmd_bss_kick_bots", Emulator.getTexts().getValue("commands.keys.cmd_bss_kick_bots").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = BssRoomCommandSupport.ownerRoom(gameClient);
        if (room == null) return true;
        List<Bot> bots = new ArrayList<>(room.getCurrentBots().values());
        for (Bot bot : bots) {
            Emulator.getGameEnvironment().getBotManager().pickUpBot(bot, gameClient.getHabbo());
        }
        BssRoomCommandSupport.notifyCount(gameClient, permission, bots.size());
        return true;
    }
}

final class BssRegenerateMapsCommand extends Command {
    BssRegenerateMapsCommand() {
        super("cmd_bss_regen_maps", Emulator.getTexts().getValue("commands.keys.cmd_bss_regen_maps").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = BssRoomCommandSupport.ownerRoom(gameClient);
        if (room == null) return true;
        List<RoomTile> tiles = new ArrayList<>();
        for (short x = 0; x < room.getLayout().getMapSizeX(); x++) {
            for (short y = 0; y < room.getLayout().getMapSizeY(); y++) {
                RoomTile tile = room.getLayout().getTile(x, y);
                if (tile != null && tile.getState() != RoomTileState.INVALID) tiles.add(tile);
            }
        }
        room.updateTiles(tiles);
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_regen_maps")
                        .replace("%count%", Integer.toString(tiles.size())),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssCloseDiceCommand extends Command {
    BssCloseDiceCommand() {
        super("cmd_bss_close_dice", Emulator.getTexts().getValue("commands.keys.cmd_bss_close_dice").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = BssRoomCommandSupport.ownerRoom(gameClient);
        if (room == null) return true;
        int count = 0;
        for (HabboItem item : List.copyOf(room.getFloorItems())) {
            if (!(item instanceof InteractionDice) || "-1".equals(item.getExtradata())) continue;
            item.setExtradata("0");
            item.needsUpdate(true);
            Emulator.getThreading().run(item);
            room.updateItem(item);
            count++;
        }
        BssRoomCommandSupport.notifyCount(gameClient, permission, count);
        return true;
    }
}

final class BssRoomStateCommand extends Command {
    private final RoomState targetState;

    BssRoomStateCommand(String permission, RoomState targetState) {
        super(permission, Emulator.getTexts().getValue("commands.keys." + permission).split(";"));
        this.targetState = targetState;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;
        room.setState(targetState);
        room.setNeedsUpdate(true);
        room.save();
        room.sendComposer(new RoomSettingsUpdatedComposer(room).compose());
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success." + permission), RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssDisableEffectCommand extends Command {
    BssDisableEffectCommand() {
        super("cmd_bss_disable_effect", Emulator.getTexts().getValue("commands.keys.cmd_bss_disable_effect").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room != null) room.giveEffect(gameClient.getHabbo(), 0, -1);
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_disable_effect"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssReloadCreditsCommand extends Command {
    BssReloadCreditsCommand() {
        super("cmd_bss_reload_credits", Emulator.getTexts().getValue("commands.keys.cmd_bss_reload_credits").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        long before = habbo.getHabboInfo().getCreditsLong();
        if (before <= 0) {
            int amount = Math.max(0, Emulator.getConfig().getInt("bss.commands.reload_credits.amount", 5000));
            habbo.getHabboInfo().setCredits(amount);
            gameClient.sendResponse(new UserCreditsComposer(habbo));
        }
        habbo.whisper(
                Emulator.getTexts().getValue(before <= 0
                                ? "commands.success.cmd_bss_reload_credits.changed"
                                : "commands.success.cmd_bss_reload_credits.unchanged")
                        .replace("%credits%", Long.toString(habbo.getHabboInfo().getCreditsLong())),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssRoomBanCommand extends Command {
    BssRoomBanCommand() {
        super("cmd_bss_room_ban", Emulator.getTexts().getValue("commands.keys.cmd_bss_room_ban").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = BssRoomCommandSupport.ownerRoom(gameClient);
        if (room == null) return true;
        if (params.length != 2) return usage(gameClient);
        Habbo target = room.getHabbo(params[1]);
        if (target == null || target == gameClient.getHabbo()) return usage(gameClient);
        Emulator.getGameEnvironment()
                .getRoomManager()
                .banUserFromRoom(
                        gameClient.getHabbo(),
                        target.getHabboInfo().getId(),
                        room.getId(),
                        RoomManager.RoomBanTypes.RWUAM_BAN_USER_HOUR);
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_room_ban")
                        .replace("%user%", target.getHabboInfo().getUsername()),
                RoomChatMessageBubbles.ALERT);
        return true;
    }

    private boolean usage(GameClient gameClient) {
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.error.cmd_bss_room_ban.usage"),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssTogglePyramidsCommand extends Command {
    BssTogglePyramidsCommand() {
        super("cmd_bss_toggle_pyramids", Emulator.getTexts().getValue("commands.keys.cmd_bss_toggle_pyramids").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = BssRoomCommandSupport.ownerRoom(gameClient);
        if (room == null) return true;
        int ownerId = gameClient.getHabbo().getHabboInfo().getId();
        List<HabboItem> pyramids = room.getFloorItems().stream()
                .filter(item -> item instanceof InteractionPyramid
                        && item.getUserId() == ownerId
                        && "wf_pyramid".equalsIgnoreCase(item.getBaseItem().getName()))
                .toList();
        boolean visible = pyramids.stream().noneMatch(item -> "1".equals(item.getExtradata()));
        for (HabboItem pyramid : pyramids) {
            pyramid.setExtradata(visible ? "1" : "0");
            pyramid.needsUpdate(true);
            Emulator.getThreading().run(pyramid);
            room.updateItem(pyramid);
        }
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success.cmd_bss_toggle_pyramids."
                        + (visible ? "visible" : "hidden")),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}

final class BssRoomCommandSupport {
    private BssRoomCommandSupport() {}

    static Room ownerRoom(GameClient gameClient) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room != null && room.isOwner(gameClient.getHabbo())) return room;
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.error.room_owner_only"),
                RoomChatMessageBubbles.ALERT);
        return null;
    }

    static void notifyCount(GameClient gameClient, String permission, int count) {
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success." + permission)
                        .replace("%count%", Integer.toString(count)),
                RoomChatMessageBubbles.ALERT);
    }
}
