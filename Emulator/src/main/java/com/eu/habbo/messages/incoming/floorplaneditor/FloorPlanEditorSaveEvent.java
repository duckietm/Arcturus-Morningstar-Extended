package com.eu.habbo.messages.incoming.floorplaneditor;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.CustomRoomLayout;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericAlertComposer;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.rooms.ForwardToRoomComposer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloorPlanEditorSaveEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FloorPlanEditorSaveEvent.class);

    public static volatile int MAXIMUM_FLOORPLAN_WIDTH_LENGTH = 64;
    public static volatile int MAXIMUM_FLOORPLAN_SIZE = 64 * 64;

    private static final int SAVE_COOLDOWN_SECONDS = 3;
    private static final int MAX_AUTO_PICKUP_ITEMS = 5000;
    private static final Pattern ALLOWED_MAP_CHARS = Pattern.compile("[a-zA-Z0-9\r]+");

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null) return;

        int habboId = this.client.getHabbo().getHabboInfo().getId();
        boolean isRoomOwner = room.getOwnerId() == habboId;
        boolean hasFloorplanPermission =
                this.client.getHabbo().hasPermission(Permission.ACC_FLOORPLAN_EDITOR);

        // The actual room owner can always edit/save their own floorplan.
        // Staff with ACC_FLOORPLAN_EDITOR can edit/save floorplans in any room.
        if (!isRoomOwner && !hasFloorplanPermission) {
            this.client.sendResponse(
                    new GenericAlertComposer(Emulator.getTexts().getValue("floorplan.permission")));
            return;
        }

        long now = Emulator.getIntUnixTimestamp();
        if (now - this.client.getHabbo().getHabboStats().lastFloorplanSaveTimestamp < SAVE_COOLDOWN_SECONDS) {
            this.client.sendResponse(new BubbleAlertComposer(
                    BubbleAlertKeys.FLOORPLAN_EDITOR_ERROR.key, "Please wait a few seconds before saving again."));
            return;
        }

        StringJoiner errors = new StringJoiner("<br />");
        String map = this.packet.readString();

        if (map == null || map.isEmpty()) {
            LOGGER.warn(
                    "Floorplan save rejected (empty map): user={} room={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId());
            return;
        }

        /*
         * Normalize imported floorplans.
         *
         * Nitro normally sends rows separated by CR (\r), but copied/exported
         * maps may contain Windows CRLF, Unix LF, or spaces instead of newlines.
         */
        map = map
                .replace("\r\n", "\r")
                .replace('\n', '\r')
                .replace("X", "x");

        /*
         * If the imported text contains no real line separators but contains
         * spaces, treat runs of spaces as row separators. This supports exports
         * copied through browsers/chat/editors that flatten newlines to spaces.
         */
        if (map.indexOf('\r') < 0 && map.indexOf(' ') >= 0) {
            map = map.trim().replaceAll(" +", "\r");
        }

        if (!ALLOWED_MAP_CHARS.matcher(map).matches()) {
            LOGGER.warn(
                    "Floorplan save rejected (illegal chars): user={} room={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId());
            return;
        }

        String[] mapRows = map.split("\r");

        if (mapRows.length == 0 || mapRows.length > MAXIMUM_FLOORPLAN_WIDTH_LENGTH) {
            LOGGER.warn(
                    "Floorplan save rejected (invalid row count): user={} room={} rows={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId(),
                    mapRows.length);
            return;
        }

        int firstRowSize = mapRows[0].length();

        if (firstRowSize == 0 || firstRowSize > MAXIMUM_FLOORPLAN_WIDTH_LENGTH) {
            LOGGER.warn(
                    "Floorplan save rejected (invalid width): user={} room={} width={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId(),
                    firstRowSize);
            return;
        }

        int cellCount = 0;

        for (String row : mapRows) {
            if (row.length() != firstRowSize) {
                LOGGER.warn(
                        "Floorplan save rejected (non-rectangular map): user={} room={} expectedWidth={} actualWidth={}",
                        this.client.getHabbo().getHabboInfo().getId(),
                        room.getId(),
                        firstRowSize,
                        row.length());
                return;
            }

            cellCount += row.length();
        }

        if (cellCount > MAXIMUM_FLOORPLAN_SIZE) {
            LOGGER.warn(
                    "Floorplan save rejected (too many cells): user={} room={} cells={} max={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId(),
                    cellCount,
                    MAXIMUM_FLOORPLAN_SIZE);
            return;
        }

        if (Emulator.getConfig().getBoolean("hotel.room.floorplan.check.enabled")) {
            if (map.replace("x", "").replace("\r", "").isEmpty()) {
                errors.add("${notification.floorplan_editor.error.message.effective_height_is_0}");
            }
        }

        int doorX = this.packet.readInt();
        int doorY = this.packet.readInt();

        if (doorX < 0 || doorX >= firstRowSize || doorY < 0 || doorY >= mapRows.length) {
            errors.add("${notification.floorplan_editor.error.message.entry_tile_outside_map}");
        } else if (mapRows[doorY].charAt(doorX) == 'x') {
            errors.add("${notification.floorplan_editor.error.message.entry_not_on_tile}");
        }

        int doorRotation = this.packet.readInt();
        if (doorRotation < 0 || doorRotation > 7) {
            errors.add("${notification.floorplan_editor.error.message.invalid_entry_tile_direction}");
        }

        int wallSize = this.packet.readInt();
        if (wallSize < -2 || wallSize > 1) {
            errors.add("${notification.floorplan_editor.error.message.invalid_wall_thickness}");
        }
        int floorSize = this.packet.readInt();
        if (floorSize < -2 || floorSize > 1) {
            errors.add("${notification.floorplan_editor.error.message.invalid_floor_thickness}");
        }

        int wallHeight = -1;
        if (this.packet.bytesAvailable() >= 4) wallHeight = this.packet.readInt();

        if (wallHeight < -1 || wallHeight > 15) {
            errors.add("${notification.floorplan_editor.error.message.invalid_walls_fixed_height}");
        }

        boolean autoPickup = false;
        if (this.packet.bytesAvailable() >= 1) {
            autoPickup = this.packet.readBoolean();
        }

        if (errors.length() > 0) {
            this.client.sendResponse(
                    new BubbleAlertComposer(BubbleAlertKeys.FLOORPLAN_EDITOR_ERROR.key, errors.toString()));
            return;
        }

        // The map currently in use, row by row, so unchanged tiles can be skipped below.
        String previousMap = room.getLayout() != null && room.getLayout().getHeightmap() != null
                ? room.getLayout().getHeightmap()
                : "";
        String[] previousRows = previousMap.replace("\r\n", "\r").replace('\n', '\r').split("\r");

        Set<RoomTile> locked_tileList = room.getLockedTiles();
        Set<RoomTile> new_tileList = new HashSet<>();
        Set<HabboItem> itemsToPickup = new HashSet<>();
        int blockedX = -1;
        int blockedY = -1;
        blockingRoomItemScan:
        for (int y = 0; y < mapRows.length; y++) {
            for (int x = 0; x < firstRowSize; x++) {

                RoomTile tile = room.getLayout().getTile((short) x, (short) y);
                new_tileList.add(tile);

                // A tile that keeps exactly the same character cannot block the save: the furniture already
                // standing there (even on a void tile of an imported room) is not affected by this change. Only
                // tiles that really change height or become void are checked below.
                char previous = (y < previousRows.length && x < previousRows[y].length())
                        ? Character.toLowerCase(previousRows[y].charAt(x))
                        : 'x';
                if (previous == Character.toLowerCase(mapRows[y].charAt(x))) continue;

                String square = String.valueOf(mapRows[y].charAt(x));
                short height;

                if (square.equalsIgnoreCase("x") && room.getTopItemAt(x, y) != null) {
                    if (autoPickup) {
                        Set<HabboItem> here = room.getItemsAt(x, y);
                        if (here != null) itemsToPickup.addAll(here);
                        continue;
                    }
                    blockedX = x;
                    blockedY = y;
                    break blockingRoomItemScan;
                }

                try {
                    if (square.isEmpty()) {
                        height = 0;
                    } else if (Emulator.isNumeric(square)) {
                        height = Short.parseShort(square);
                    } else {
                        int idx = "abcdefghijklmnopqrstuvwxyz".indexOf(square.toLowerCase());
                        if (idx < 0) {
                            return;
                        }
                        height = (short) Math.min(26, 10 + idx);
                    }
                } catch (NumberFormatException e) {
                    return;
                }

                if (tile != null
                        && tile.state != RoomTileState.INVALID
                        && height != tile.z
                        && room.getTopItemAt(x, y) != null) {
                    if (autoPickup) {
                        Set<HabboItem> here = room.getItemsAt(x, y);
                        if (here != null) itemsToPickup.addAll(here);
                        continue;
                    }
                    blockedX = x;
                    blockedY = y;
                    break blockingRoomItemScan;
                }
            }
        }

        if (blockedX < 0) {
            locked_tileList.removeAll(new_tileList);
            if (!locked_tileList.isEmpty()) {
                if (autoPickup) {
                    for (RoomTile lt : locked_tileList) {
                        Set<HabboItem> here = room.getItemsAt(lt.x, lt.y);
                        if (here != null) itemsToPickup.addAll(here);
                    }
                } else {
                    RoomTile first = locked_tileList.iterator().next();
                    blockedX = first.x;
                    blockedY = first.y;
                }
            }
        }

        if (blockedX >= 0) {
            this.client.sendResponse(new BubbleAlertComposer(
                    BubbleAlertKeys.FLOORPLAN_EDITOR_ERROR.key,
                    "${notification.floorplan_editor.error.message.change_blocked_by_room_item} (" + blockedX + ", "
                            + blockedY + ")"));
            return;
        }

        if (autoPickup && !itemsToPickup.isEmpty()) {
            if (itemsToPickup.size() > MAX_AUTO_PICKUP_ITEMS) {
                LOGGER.warn(
                        "Floorplan auto-pickup rejected (over cap): user={} room={} itemCount={} cap={}",
                        this.client.getHabbo().getHabboInfo().getId(),
                        room.getId(),
                        itemsToPickup.size(),
                        MAX_AUTO_PICKUP_ITEMS);
                this.client.sendResponse(new BubbleAlertComposer(
                        BubbleAlertKeys.FLOORPLAN_EDITOR_ERROR.key,
                        "Too many items would be picked up (" + itemsToPickup.size() + " > " + MAX_AUTO_PICKUP_ITEMS
                                + "). Remove some furniture manually and save again."));
                return;
            }

            Map<Integer, ArrayList<HabboItem>> byOwner = new HashMap<>();
            for (HabboItem itm : itemsToPickup) {
                if (itm == null) continue;
                byOwner.computeIfAbsent(itm.getUserId(), k -> new ArrayList<>()).add(itm);
                room.pickUpItem(itm, null);
            }

            for (Map.Entry<Integer, ArrayList<HabboItem>> entry : byOwner.entrySet()) {
                Habbo owner = Emulator.getGameEnvironment().getHabboManager().getHabbo(entry.getKey());
                if (owner == null) continue;
                for (HabboItem itm : entry.getValue()) {
                    owner.getClient().sendResponse(new AddHabboItemComposer(itm));
                }
                owner.getClient().sendResponse(new InventoryRefreshComposer());
            }

            LOGGER.info(
                    "Floorplan auto-pickup: user={} room={} itemCount={} owners={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId(),
                    itemsToPickup.size(),
                    byOwner.size());
        }

        RoomLayout layout = room.getLayout();

        if (layout instanceof CustomRoomLayout) {
            layout.setDoorX((short) doorX);
            layout.setDoorY((short) doorY);
            layout.setDoorDirection(doorRotation);
            layout.setHeightmap(map);
            layout.parse();

            if (layout.getDoorTile() == null) {
                this.client.getHabbo().alert("Error");
                ((CustomRoomLayout) layout).needsUpdate(false);
                Emulator.getGameEnvironment().getRoomManager().unloadRoom(room);
                return;
            }
            ((CustomRoomLayout) layout).needsUpdate(true);
            Emulator.getThreading().run((CustomRoomLayout) layout);
        } else {
            layout = Emulator.getGameEnvironment()
                    .getRoomManager()
                    .insertCustomLayout(room, map, doorX, doorY, doorRotation);
        }

        if (layout != null) {
            room.setHasCustomLayout(true);
            room.setNeedsUpdate(true);
            room.setLayout(layout);
            room.setWallSize(wallSize);
            room.setFloorSize(floorSize);
            room.setWallHeight(wallHeight);
            room.save();

            this.client.getHabbo().getHabboStats().lastFloorplanSaveTimestamp = now;
            LOGGER.info(
                    "Floorplan saved: user={} room={} mapLen={} rows={} cols={}",
                    this.client.getHabbo().getHabboInfo().getId(),
                    room.getId(),
                    map.length(),
                    mapRows.length,
                    firstRowSize);

            Collection<Habbo> habbos = new ArrayList<>(room.getUserCount());
            habbos.addAll(room.getHabbos());
            Emulator.getGameEnvironment().getRoomManager().unloadRoom(room);
            room = Emulator.getGameEnvironment().getRoomManager().loadRoom(room.getId());
            ServerMessage message = new ForwardToRoomComposer(room.getId()).compose();
            for (Habbo habbo : habbos) {
                habbo.getClient().sendResponse(message);
            }
        }
    }
}
