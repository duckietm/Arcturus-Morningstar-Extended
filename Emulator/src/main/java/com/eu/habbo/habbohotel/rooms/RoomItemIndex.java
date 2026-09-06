package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.habbohotel.items.FurniFootprint;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.interactions.InteractionPostIt;
import com.eu.habbo.habbohotel.users.HabboItem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongMaps;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class RoomItemIndex {

    private final Room room;
    private final Int2ObjectMap<HabboItem> items = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(0));
    private final Int2ObjectMap<String> ownerNames = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(0));
    private final Int2IntMap ownerCounts = Int2IntMaps.synchronize(new Int2IntOpenHashMap(0));
    private final Int2LongMap itemIncarnations = Int2LongMaps.synchronize(new Int2LongOpenHashMap(0));
    private final AtomicLong incarnationSequence = new AtomicLong();
    private final ConcurrentHashMap<RoomTile, Set<HabboItem>> tileCache = new ConcurrentHashMap<>();

    RoomItemIndex(Room room) {
        this.room = room;
    }

    Int2ObjectMap<HabboItem> items() {
        return this.items;
    }

    Int2ObjectMap<String> ownerNames() {
        return this.ownerNames;
    }

    Int2IntMap ownerCounts() {
        return this.ownerCounts;
    }

    ConcurrentHashMap<RoomTile, Set<HabboItem>> tileCache() {
        return this.tileCache;
    }

    HabboItem get(int id) {
        if (this.room.getRoomSpecialTypes() == null) {
            return null;
        }

        HabboItem item;
        synchronized (this.items) {
            item = this.items.get(id);
        }

        return item != null ? item : this.room.getRoomSpecialTypes().getSpecialItem(id);
    }

    long registerIncarnation(HabboItem item) {
        if (item == null) {
            return 0L;
        }
        long incarnation = this.incarnationSequence.incrementAndGet();
        this.itemIncarnations.put(item.getId(), incarnation);
        return incarnation;
    }

    void unregisterIncarnation(HabboItem item) {
        if (item != null) {
            this.itemIncarnations.remove(item.getId());
        }
    }

    long itemIncarnation(int itemId) {
        return this.itemIncarnations.getOrDefault(itemId, 0L);
    }

    int size() {
        return this.items.size();
    }

    Set<HabboItem> floorItems() {
        return this.itemsOfType(FurnitureType.FLOOR);
    }

    Set<HabboItem> wallItems() {
        return this.itemsOfType(FurnitureType.WALL);
    }

    Set<HabboItem> postItNotes() {
        Set<HabboItem> result = new HashSet<>();
        synchronized (this.items) {
            for (HabboItem item : this.items.values()) {
                if (item.getBaseItem().getInteractionType().getType() == InteractionPostIt.class) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    Set<HabboItem> itemsAt(RoomTile tile, boolean returnOnFirst) {
        Set<HabboItem> result = new HashSet<>(0);
        if (tile == null) {
            return result;
        }

        if (this.room.isLoaded()) {
            Set<HabboItem> cachedItems = this.tileCache.get(tile);
            if (cachedItems != null) {
                return cachedItems;
            }
        }

        synchronized (this.items) {
            for (HabboItem item : this.items.values()) {
                if (item == null || item.getBaseItem().getType() != FurnitureType.FLOOR) {
                    continue;
                }

                int width;
                int length;
                if (item.getRotation() != 2 && item.getRotation() != 6) {
                    width = Math.max(item.getBaseItem().getWidth(), 1);
                    length = Math.max(item.getBaseItem().getLength(), 1);
                } else {
                    width = Math.max(item.getBaseItem().getLength(), 1);
                    length = Math.max(item.getBaseItem().getWidth(), 1);
                }

                // A shape can be anchored off its furni's tile - the artwork of plenty of custom furni
                // spills up and left of where they are placed - so the rectangle to test against starts at
                // the anchor, not at the item.
                FurniFootprint footprint = item.getBaseItem().getFootprint();
                int[] anchor = footprint.anchor(item.getRotation());

                if (tile.x < item.getX() + anchor[0]
                        || tile.x > item.getX() + anchor[0] + width - 1
                        || tile.y < item.getY() + anchor[1]
                        || tile.y > item.getY() + anchor[1] + length - 1) {
                    continue;
                }

                // Inside the bounding rectangle, but a furni shaped in the editor does not fill it.
                if (footprint.isCustom()
                        && item.getRotation() % 2 == 0
                        && !footprint.isOccupied(item.getRotation(), tile.x - item.getX(), tile.y - item.getY())) {
                    continue;
                }

                result.add(item);
                if (returnOnFirst) {
                    return result;
                }
            }
        }

        if (this.room.isLoaded()) {
            this.tileCache.put(tile, result);
        }
        return result;
    }

    /**
     * Ordered by item id, which is the order the furni were placed in.
     *
     * The client breaks a depth tie between two overlapping furni by the order it received them in, so a
     * HashSet here painted the same room differently on every load - a rug over a floor sticker one time
     * and under it the next. Placement order is both stable and the order a builder expects.
     */
    private Set<HabboItem> itemsOfType(FurnitureType type) {
        List<HabboItem> matches = new ArrayList<>();
        synchronized (this.items) {
            for (HabboItem item : this.items.values()) {
                if (item.getBaseItem().getType() == type) {
                    matches.add(item);
                }
            }
        }
        matches.sort(Comparator.comparingInt(HabboItem::getId));
        return new LinkedHashSet<>(matches);
    }

    void clear() {
        synchronized (this.items) {
            this.items.clear();
        }
        synchronized (this.ownerCounts) {
            this.ownerCounts.clear();
        }
        synchronized (this.ownerNames) {
            this.ownerNames.clear();
        }
        this.itemIncarnations.clear();
        this.tileCache.clear();
    }
}
