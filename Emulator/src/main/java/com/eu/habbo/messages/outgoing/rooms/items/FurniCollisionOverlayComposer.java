package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The tiles every furni in the room actually blocks, so the client can draw them over the floor.
 *
 * <p>The tiles are the ones {@link com.eu.habbo.habbohotel.rooms.RoomLayout#getTilesAt} hands the
 * movement checks - the same set the engine walks a unit around - rather than a width x length
 * rectangle recomputed for display. A furni whose artwork does not sit over its collision shows up
 * as exactly that: markers beside the picture instead of under it.
 */
public class FurniCollisionOverlayComposer extends MessageComposer {

    private final boolean active;
    private final Map<HabboItem, Set<RoomTile>> tiles;

    public FurniCollisionOverlayComposer(boolean active, Map<HabboItem, Set<RoomTile>> tiles) {
        this.active = active;
        this.tiles = tiles;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FurniCollisionOverlayComposer);
        this.response.appendBoolean(this.active);

        int count = 0;
        for (Set<RoomTile> occupied : this.tiles.values()) {
            count += occupied.size();
        }

        this.response.appendInt(count);

        for (Map.Entry<HabboItem, Set<RoomTile>> entry : this.tiles.entrySet()) {
            HabboItem item = entry.getKey();
            String name = item.getBaseItem() != null ? item.getBaseItem().getName() : "";
            int width = item.getBaseItem() != null ? item.getBaseItem().getWidth() : 1;
            int length = item.getBaseItem() != null ? item.getBaseItem().getLength() : 1;

            for (RoomTile tile : entry.getValue()) {
                this.response.appendInt(item.getId());
                this.response.appendString(name);
                this.response.appendInt(tile.x);
                this.response.appendInt(tile.y);
                this.response.appendString(String.valueOf(tile.getStackHeight()));
                // The furni's own tile is drawn differently: it is the one the client thinks it is on.
                this.response.appendBoolean(tile.x == item.getX() && tile.y == item.getY());
                this.response.appendInt(item.getRotation());
                this.response.appendInt(width);
                this.response.appendInt(length);
            }
        }

        return this.response;
    }

    /** An empty overlay, which is how the client is told to clear what it drew. */
    public static FurniCollisionOverlayComposer cleared() {
        return new FurniCollisionOverlayComposer(false, Map.of());
    }

    /** @return the item ids carried by this overlay, for logging. */
    public List<Integer> itemIds() {
        return this.tiles.keySet().stream().map(HabboItem::getId).toList();
    }
}
