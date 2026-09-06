package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemUpdateComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-room "auto stack height" option ({@code :autostackheight}, default OFF, table
 * {@code room_auto_stack}). When enabled, moved/rotated furniture snaps to the stack
 * height of its destination and furniture left floating after the item beneath it moves
 * or is picked up drops down to the new stack height.
 *
 * A room only gets the behaviour once someone turns it on, so building by hand keeps
 * the height a furni was placed at.
 */
public final class RoomAutoStackSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomAutoStackSupport.class);
    private static final Map<Integer, Boolean> CACHE = new ConcurrentHashMap<>();
    private static final double EPSILON = 0.0001D;

    private RoomAutoStackSupport() {}

    public static boolean isEnabled(Room room) {
        if (room == null) return false;
        return CACHE.computeIfAbsent(room.getId(), RoomAutoStackSupport::load);
    }

    private static Boolean load(int roomId) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT enabled FROM room_auto_stack WHERE room_id = ?")) {
            statement.setInt(1, roomId);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) return set.getInt("enabled") == 1;
            }
        } catch (SQLException e) {
            LOGGER.warn("room_auto_stack read failed for room {}", roomId, e);
        }
        // No row (and a failed read) means the room never opted in.
        return Boolean.FALSE;
    }

    public static void set(Room room, boolean enabled) {
        if (room == null) return;
        CACHE.put(room.getId(), enabled);
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO room_auto_stack (room_id, enabled) VALUES (?, ?) ON DUPLICATE KEY UPDATE enabled = VALUES(enabled)")) {
            statement.setInt(1, room.getId());
            statement.setInt(2, enabled ? 1 : 0);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.warn("room_auto_stack write failed for room {}", room.getId(), e);
        }
    }

    /**
     * Drops every item that was resting at or above {@code fromZ} on {@code tiles} down to the
     * current stack height (bottom-up, so stacks cascade), broadcasting the new positions.
     */
    public static void settleAbove(Room room, Collection<RoomTile> tiles, double fromZ, HabboItem moved) {
        if (room == null || tiles == null || tiles.isEmpty() || !isEnabled(room)) return;
        RoomLayout layout = room.getLayout();
        if (layout == null) return;

        Map<Integer, HabboItem> candidates = new LinkedHashMap<>();
        for (RoomTile tile : tiles) {
            if (tile == null) continue;
            for (HabboItem item : room.getItemManager().getItemsAt(tile)) {
                if (item == null || item == moved) continue;
                if (item.getZ() + EPSILON >= fromZ) candidates.putIfAbsent(item.getId(), item);
            }
        }
        if (candidates.isEmpty()) return;

        List<HabboItem> ordered = new ArrayList<>(candidates.values());
        ordered.sort(Comparator.comparingDouble(HabboItem::getZ));

        Set<RoomTile> touched = new HashSet<>();
        for (HabboItem item : ordered) {
            RoomTile base = layout.getTile(item.getX(), item.getY());
            if (base == null) continue;
            Set<RoomTile> footprint = layout.getTilesAt(base, item);

            double target = layout.getHeightAtSquare(base.x, base.y);
            boolean blocked = false;
            for (RoomTile f : footprint) {
                double h = room.getStackHeight(f.x, f.y, false, item);
                if (h < 0) { blocked = true; break; }
                target = Math.max(target, h);
            }
            if (blocked || target >= item.getZ() - EPSILON) continue;

            item.setZ(target);
            item.needsUpdate(true);
            Emulator.getThreading().run(item);
            room.sendComposer(new FloorItemUpdateComposer(item).compose());
            touched.addAll(footprint);
        }
        if (!touched.isEmpty()) room.updateTiles(touched);
    }
}
