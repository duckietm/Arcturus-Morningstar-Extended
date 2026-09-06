package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.TreeMap;

/**
 * RCON diagnostic for walkability problems: {@code {"key":"tilestate","data":{"room_id":32,"x":20,"y":20}}}.
 *
 * <p>Loads the room if needed and answers with a JSON report in {@code message}: tile states of the whole map,
 * the tiles whose stored state differs from a fresh calculation (stale after load), and, when x/y are given, the
 * full detail of that tile with every item standing on it (z, height, walkable flags, interaction class).
 */
public class TileState extends RCONMessage<TileState.JSONTileState> {

    public TileState() {
        super(JSONTileState.class);
    }

    @Override
    public void handle(Gson gson, JSONTileState json) {
        Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(json.room_id, true);
        if (room == null) {
            this.status = ROOM_NOT_FOUND;
            this.message = "room " + json.room_id + " not found";
            return;
        }

        for (int i = 0; i < 150 && !room.isLoaded(); i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        RoomLayout layout = room.getLayout();
        if (layout == null) {
            this.status = STATUS_ERROR;
            this.message = "room " + json.room_id + " has no layout";
            return;
        }

        JsonObject report = new JsonObject();
        report.addProperty("room_id", room.getId());
        report.addProperty("loaded", room.isLoaded());
        report.addProperty("items", room.getItemManager().itemCount());
        report.addProperty("map", layout.getMapSizeX() + "x" + layout.getMapSizeY());
        report.addProperty("door", layout.getDoorX() + "," + layout.getDoorY());
        report.addProperty("allow_underpass", room.isAllowUnderpass());

        Map<String, Integer> states = new TreeMap<>();
        int stale = 0;
        int tilesWithItems = 0;
        JsonArray staleSamples = new JsonArray();
        for (short x = 0; x < layout.getMapSizeX(); x++) {
            for (short y = 0; y < layout.getMapSizeY(); y++) {
                RoomTile tile = layout.getTile(x, y);
                if (tile == null) continue;

                states.merge(tile.getState().name(), 1, Integer::sum);
                if (tile.getState() == RoomTileState.INVALID) continue;

                if (!room.getItemManager().getItemsAt(tile).isEmpty()) tilesWithItems++;

                RoomTileState fresh = room.calculateTileState(tile);
                if (fresh != tile.getState()) {
                    stale++;
                    if (staleSamples.size() < 20) staleSamples.add(describe(room, tile, fresh));
                }
            }
        }

        report.add("states", gson.toJsonTree(states));
        report.addProperty("tiles_with_items", tilesWithItems);
        report.addProperty("stale_tiles", stale);
        report.add("stale_samples", staleSamples);

        if (json.x >= 0 && json.y >= 0) {
            RoomTile tile = layout.getTile((short) json.x, (short) json.y);
            report.add("tile", tile == null ? JsonNull.INSTANCE : describe(room, tile, room.calculateTileState(tile)));
        }

        this.message = gson.toJson(report);
    }

    private static JsonObject describe(Room room, RoomTile tile, RoomTileState fresh) {
        JsonObject result = new JsonObject();
        result.addProperty("x", tile.x);
        result.addProperty("y", tile.y);
        result.addProperty("floor", tile.z);
        result.addProperty("state", tile.getState().name());
        result.addProperty("fresh_state", fresh.name());
        result.addProperty("stack_height", tile.getStackHeight());
        result.addProperty("units", tile.getUnits().size());

        JsonArray items = new JsonArray();
        for (HabboItem item : room.getItemManager().getItemsAt(tile)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", item.getId());
            entry.addProperty("base", item.getBaseItem().getId());
            entry.addProperty("name", item.getBaseItem().getName());
            entry.addProperty("x", item.getX());
            entry.addProperty("y", item.getY());
            entry.addProperty("z", item.getZ());
            entry.addProperty("rot", item.getRotation());
            entry.addProperty("height", Item.getCurrentHeight(item));
            entry.addProperty("walkable", item.isWalkable());
            entry.addProperty("allow_walk", item.getBaseItem().allowWalk());
            entry.addProperty("allow_stack", item.getBaseItem().allowStack());
            entry.addProperty("allow_sit", item.getBaseItem().allowSit());
            entry.addProperty("allow_lay", item.getBaseItem().allowLay());
            entry.addProperty("class", item.getClass().getSimpleName());
            entry.addProperty("extradata", item.getExtradata());
            items.add(entry);
        }
        result.add("items", items);

        return result;
    }

    public static class JSONTileState {
        public int room_id;
        public int x = -1;
        public int y = -1;
    }
}
