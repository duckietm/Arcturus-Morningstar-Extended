package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomConfInvisSupport;
import com.eu.habbo.habbohotel.users.HabboItem;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClickInvisibleTilesCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickInvisibleTilesCommand.class);

    public ClickInvisibleTilesCommand() {
        super("cmd_click_invisible_tiles", Emulator.getTexts().getValue("commands.keys.cmd_click_invisible_tiles").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (!room.isOwner(gameClient.getHabbo())) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.room_owner_only"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        Set<Integer> invisibleFurnitureIds = invisibleFurnitureIds();
        String requestedState = requestedState(params);
        int updated = 0;
        List<HabboItem> roomItems = new java.util.ArrayList<>(room.getFloorItems());
        roomItems.addAll(room.getWallItems());
        for (HabboItem item : roomItems) {
            if (item == null
                    || item.getBaseItem() == null
                    || !invisibleFurnitureIds.contains(item.getBaseItem().getId())) continue;
            try {
                if (requestedState == null) {
                    item.onClick(gameClient, room, new Object[] {0});
                } else if (!requestedState.equals(item.getExtradata())) {
                    item.setExtradata(requestedState);
                    item.needsUpdate(true);
                    room.updateItemState(item);
                }
                updated++;
            } catch (Exception exception) {
                LOGGER.error("Failed to click invisible-category furniture {}", item.getId(), exception);
            }
        }

        gameClient.getHabbo().whisper(
                Emulator.getTexts()
                        .getValue("commands.success.cmd_click_invisible_tiles")
                        .replace("%count%", Integer.toString(updated)),
                RoomChatMessageBubbles.ALERT);
        return true;
    }

    private static String requestedState(String[] params) {
        if (params.length < 2) return null;
        if (params[1].equalsIgnoreCase("hide")) return "1";
        if (params[1].equalsIgnoreCase("show")) return "0";
        return null;
    }

    static Set<Integer> invisibleFurnitureIds() {
        Set<Integer> itemIds = new HashSet<>();
        for (CatalogPage page : Emulator.getGameEnvironment().getCatalogManager().catalogPages.values()) {
            if (!isInvisibleCategory(page)) continue;
            for (CatalogItem catalogItem : page.getCatalogItems().values()) {
                catalogItem.getBaseItems().forEach(item -> itemIds.add(item.getId()));
            }
        }
        return Set.copyOf(itemIds);
    }

    static boolean isInvisibleCategory(CatalogPage page) {
        if (page == null || page.getCaption() == null) return false;
        return page.getCaption().toLowerCase(Locale.ROOT).contains("invisibil");
    }
}
