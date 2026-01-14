package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.ICycleable;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.*;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameGate;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameScoreboard;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiSphere;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiTeleporter;
import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeExitTile;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagField;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagPole;
import com.eu.habbo.habbohotel.items.interactions.pets.*;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredBlob;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.tick.WiredTickable;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.*;
import com.eu.habbo.plugin.Event;
import com.eu.habbo.plugin.events.furniture.*;
import gnu.trove.TCollections;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.THashSet;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Manages all items/furniture within a room.
 * Handles loading, adding, removing, querying, and picking up items.
 */
public class RoomItemManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomItemManager.class);

    private final Room room;

    // Item storage
    private final TIntObjectMap<HabboItem> roomItems;

    // Furniture owner tracking
    private final TIntObjectMap<String> furniOwnerNames;
    private final TIntIntMap furniOwnerCount;

    // Tile cache for item lookups
    public final ConcurrentHashMap<RoomTile, THashSet<HabboItem>> tileCache;

    public RoomItemManager(Room room) {
        this.room = room;
        this.roomItems = TCollections.synchronizedMap(new TIntObjectHashMap<>(0));
        this.furniOwnerNames = TCollections.synchronizedMap(new TIntObjectHashMap<>(0));
        this.furniOwnerCount = TCollections.synchronizedMap(new TIntIntHashMap(0));
        this.tileCache = new ConcurrentHashMap<>();
    }

    // =====================================================================
    // Small helpers to remove duplication
    // =====================================================================

    @FunctionalInterface
    private interface ItemVisitor {
        void accept(HabboItem item);
    }

    /** Safe synchronized iterator traversal (kills repeated iterator.advance() try/catch blocks). */
    private void forEachRoomItem(ItemVisitor visitor) {
        synchronized (this.roomItems) {
            TIntObjectIterator<HabboItem> it = this.roomItems.iterator();
            for (int i = this.roomItems.size(); i-- > 0; ) {
                try {
                    it.advance();
                    HabboItem item = it.value();
                    if (item != null) visitor.accept(item);
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    /** Generic collector for “loop + if + add”. */
    private THashSet<HabboItem> collect(Predicate<HabboItem> filter) {
        THashSet<HabboItem> out = new THashSet<>();
        forEachRoomItem(item -> {
            if (filter.test(item)) out.add(item);
        });
        return out;
    }

    private boolean isWiredItem(HabboItem item) {
        return item instanceof InteractionWiredTrigger
                || item instanceof InteractionWiredEffect
                || item instanceof InteractionWiredCondition
                || item instanceof InteractionWiredExtra;
    }

    private void invalidateWiredIf(boolean wired) {
        if (wired) WiredManager.invalidateRoom(this.room);
    }

    /** Update wired spatial index on move and invalidate wired cache (single place for repeated blocks). */
    private void updateWiredLocationIfNeeded(HabboItem item, RoomTile oldLocation) {
        if (oldLocation == null) return;
        RoomSpecialTypes st = this.room.getRoomSpecialTypes();
        if (st == null) return;

        boolean changed = false;

        if (item instanceof InteractionWiredTrigger) {
            st.updateTriggerLocation((InteractionWiredTrigger) item, oldLocation.x, oldLocation.y);
            changed = true;
        } else if (item instanceof InteractionWiredEffect) {
            st.updateEffectLocation((InteractionWiredEffect) item, oldLocation.x, oldLocation.y);
            changed = true;
        } else if (item instanceof InteractionWiredCondition) {
            st.updateConditionLocation((InteractionWiredCondition) item, oldLocation.x, oldLocation.y);
            changed = true;
        } else if (item instanceof InteractionWiredExtra) {
            st.updateExtraLocation((InteractionWiredExtra) item, oldLocation.x, oldLocation.y);
            changed = true;
        }

        if (changed) WiredManager.invalidateRoom(this.room);
    }

    /** Shared “send update + update tiles + update habbos/bots”. */
    private void broadcastMoveUpdate(HabboItem item,
                                     THashSet<RoomTile> occupiedTiles,
                                     THashSet<RoomTile> oldOccupiedTiles,
                                     boolean sendUpdates) {
        if (sendUpdates) {
            this.room.sendComposer(new FloorItemUpdateComposer(item).compose());
        }

        // Update old & new tiles
        occupiedTiles.removeAll(oldOccupiedTiles);
        occupiedTiles.addAll(oldOccupiedTiles);
        this.room.updateTiles(occupiedTiles);

        // Update units
        for (RoomTile t : occupiedTiles) {
            this.room.updateHabbosAt(t.x, t.y, this.room.getHabbosAt(t.x, t.y));
            this.room.updateBotsAt(t.x, t.y);
        }
    }

    private double applyBuildHeightEvent(HabboItem item, Habbo actor, RoomLayout layout, int x, int y, double currentHeight) {
        if (Emulator.getPluginManager().isRegistered(FurnitureBuildheightEvent.class, true)) {
            FurnitureBuildheightEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurnitureBuildheightEvent(item, actor, 0.00, currentHeight));
            if (event.hasChangedHeight()) {
                return layout.getHeightAtSquare(x, y) + event.getUpdatedHeight();
            }
        }
        return currentHeight;
    }

    private boolean heightSanityFails(RoomLayout layout, RoomTile tile, double z) {
        if (z > Room.MAXIMUM_FURNI_HEIGHT) return true;
        return z < layout.getHeightAtSquare(tile.x, tile.y);
    }

    // =====================================================================
    // LOADING
    // =====================================================================

    /** Loads items from the database. */
    public void loadItems(Connection connection) {
        synchronized (this.roomItems) {
            this.roomItems.clear();
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM items WHERE room_id = ?")) {
            statement.setInt(1, this.room.getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    this.addHabboItem(Emulator.getGameEnvironment().getItemManager().loadHabboItem(set));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        if (this.itemCount() > Room.MAXIMUM_FURNI) {
            LOGGER.error("Room ID: {} has exceeded the furniture limit ({} > {}).",
                    this.room.getId(), this.itemCount(), Room.MAXIMUM_FURNI);
        }
    }

    /** Loads wired data for items. */
    public void loadWiredData(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, wired_data FROM items WHERE room_id = ? AND wired_data<>''")) {
            statement.setInt(1, this.room.getId());

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    try {
                        HabboItem item = this.getHabboItem(set.getInt("id"));
                        if (item instanceof InteractionWired) {
                            ((InteractionWired) item).loadWiredData(set, this.room);
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Caught SQL exception", e);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    // =====================================================================
    // ITEM RETRIEVAL
    // =====================================================================

    /** Gets an item by ID. */
    public HabboItem getHabboItem(int id) {
        if (this.roomItems == null || this.room.getRoomSpecialTypes() == null) {
            return null;
        }

        HabboItem item;
        synchronized (this.roomItems) {
            item = this.roomItems.get(id);
        }
        if (item != null) return item;

        RoomSpecialTypes st = this.room.getRoomSpecialTypes();
        if (st == null) return null;

        // Consolidated “special types” lookup
        List<IntFunction<HabboItem>> lookups = Arrays.asList(
                st::getBanzaiTeleporter,
                st::getTrigger,
                st::getEffect,
                st::getCondition,
                st::getGameGate,
                st::getGameScorebord,
                st::getGameTimer,
                (int i) -> st.getFreezeExitTiles().get(i),
                st::getRoller,
                st::getNest,
                st::getPetDrink,
                st::getPetFood
        );

        for (IntFunction<HabboItem> fn : lookups) {
            HabboItem hit = fn.apply(id);
            if (hit != null) return hit;
        }

        return null;
    }

    /** Gets the total item count. */
    public int itemCount() {
        return this.roomItems.size();
    }

    /** Gets all floor items. */
    public THashSet<HabboItem> getFloorItems() {
        return collect(i -> i.getBaseItem().getType() == FurnitureType.FLOOR);
    }

    /** Gets all wall items. */
    public THashSet<HabboItem> getWallItems() {
        return collect(i -> i.getBaseItem().getType() == FurnitureType.WALL);
    }

    /** Gets all post-it notes. */
    public THashSet<HabboItem> getPostItNotes() {
        return collect(i -> i.getBaseItem().getInteractionType().getType() == InteractionPostIt.class);
    }

    /** Gets the room items map. */
    public TIntObjectMap<HabboItem> getRoomItems() {
        return this.roomItems;
    }

    // =====================================================================
    // ITEM POSITION QUERIES
    // =====================================================================

    @Deprecated
    public THashSet<HabboItem> getItemsAt(int x, int y) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);
        if (tile != null) return this.getItemsAt(tile);
        return new THashSet<>(0);
    }

    public THashSet<HabboItem> getItemsAt(RoomTile tile) {
        return getItemsAt(tile, false);
    }

    public THashSet<HabboItem> getItemsAt(RoomTile tile, boolean returnOnFirst) {
        THashSet<HabboItem> items = new THashSet<>(0);

        if (tile == null) return items;

        if (this.room.isLoaded()) {
            THashSet<HabboItem> cachedItems = this.tileCache.get(tile);
            if (cachedItems != null) return cachedItems;
        }

        forEachRoomItem(item -> {
            if (item.getBaseItem().getType() != FurnitureType.FLOOR) return;

            int width, length;

            if (item.getRotation() != 2 && item.getRotation() != 6) {
                width = item.getBaseItem().getWidth() > 0 ? item.getBaseItem().getWidth() : 1;
                length = item.getBaseItem().getLength() > 0 ? item.getBaseItem().getLength() : 1;
            } else {
                width = item.getBaseItem().getLength() > 0 ? item.getBaseItem().getLength() : 1;
                length = item.getBaseItem().getWidth() > 0 ? item.getBaseItem().getWidth() : 1;
            }

            if (!(tile.x >= item.getX() && tile.x <= item.getX() + width - 1 && tile.y >= item.getY()
                    && tile.y <= item.getY() + length - 1)) {
                return;
            }

            items.add(item);
        });

        if (this.room.isLoaded()) {
            this.tileCache.put(tile, items);
        }

        return items;
    }

    public THashSet<HabboItem> getItemsAt(int x, int y, double minZ) {
        THashSet<HabboItem> items = new THashSet<>();
        for (HabboItem item : this.getItemsAt(x, y)) {
            if (item.getZ() < minZ) continue;
            items.add(item);
        }
        return items;
    }

    public THashSet<HabboItem> getItemsAt(Class<? extends HabboItem> type, int x, int y) {
        THashSet<HabboItem> items = new THashSet<>();
        for (HabboItem item : this.getItemsAt(x, y)) {
            if (!item.getClass().equals(type)) continue;
            items.add(item);
        }
        return items;
    }

    public boolean hasItemsAt(int x, int y) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);
        if (tile == null) return false;
        return this.getItemsAt(tile, true).size() > 0;
    }

    public HabboItem getTopItemAt(int x, int y) {
        return this.getTopItemAt(x, y, null);
    }

    public HabboItem getTopItemAt(int x, int y, HabboItem exclude) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);
        if (tile == null) return null;

        HabboItem highestItem = null;

        for (HabboItem item : this.getItemsAt(x, y)) {
            if (exclude != null && exclude == item) continue;

            if (highestItem != null && highestItem.getZ() + Item.getCurrentHeight(highestItem)
                    > item.getZ() + Item.getCurrentHeight(item)) {
                continue;
            }

            highestItem = item;
        }

        return highestItem;
    }

    public HabboItem getTopItemAt(THashSet<RoomTile> tiles, HabboItem exclude) {
        HabboItem highestItem = null;
        for (RoomTile tile : tiles) {
            if (tile == null) continue;

            for (HabboItem item : this.getItemsAt(tile.x, tile.y)) {
                if (exclude != null && exclude == item) continue;

                if (highestItem != null && highestItem.getZ() + Item.getCurrentHeight(highestItem)
                        > item.getZ() + Item.getCurrentHeight(item)) {
                    continue;
                }

                highestItem = item;
            }
        }
        return highestItem;
    }

    public double getTopHeightAt(int x, int y) {
        HabboItem item = this.getTopItemAt(x, y);

        if (item != null) {
            return (item.getZ() + Item.getCurrentHeight(item) - (item.getBaseItem().allowSit() ? 1 : 0));
        } else {
            return this.room.getLayout().getHeightAtSquare(x, y);
        }
    }

    @Deprecated
    public HabboItem getLowestChair(int x, int y) {
        if (this.room.getLayout() == null) return null;

        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);
        if (tile != null) return this.getLowestChair(tile);

        return null;
    }

    public HabboItem getLowestChair(RoomTile tile) {
        HabboItem lowestChair = null;

        THashSet<HabboItem> items = this.getItemsAt(tile);
        if (items != null && !items.isEmpty()) {
            for (HabboItem item : items) {
                if (!item.getBaseItem().allowSit()) continue;

                if (lowestChair != null && lowestChair.getZ() < item.getZ()) continue;

                lowestChair = item;
            }
        }

        return lowestChair;
    }

    public HabboItem getTallestChair(RoomTile tile) {
        HabboItem lowestChair = null;

        THashSet<HabboItem> items = this.getItemsAt(tile);
        if (items != null && !items.isEmpty()) {
            for (HabboItem item : items) {
                if (!item.getBaseItem().allowSit()) continue;

                if (lowestChair != null && lowestChair.getZ() + Item.getCurrentHeight(lowestChair)
                        > item.getZ() + Item.getCurrentHeight(item)) {
                    continue;
                }

                lowestChair = item;
            }
        }

        return lowestChair;
    }

    // =====================================================================
    // ITEM MANIPULATION
    // =====================================================================

    public void addHabboItem(HabboItem item) {
        if (item == null) return;

        synchronized (this.roomItems) {
            try {
                this.roomItems.put(item.getId(), item);
            } catch (Exception ignored) { }
        }

        synchronized (this.furniOwnerCount) {
            this.furniOwnerCount.put(item.getUserId(), this.furniOwnerCount.get(item.getUserId()) + 1);
        }

        synchronized (this.furniOwnerNames) {
            if (!this.furniOwnerNames.containsKey(item.getUserId())) {
                HabboInfo habbo = HabboManager.getOfflineHabboInfo(item.getUserId());
                if (habbo != null) {
                    this.furniOwnerNames.put(item.getUserId(), habbo.getUsername());
                } else {
                    LOGGER.error("Failed to find username for item (ID: {}, UserID: {})",
                            item.getId(), item.getUserId());
                }
            }
        }

        this.registerItemWithSpecialTypes(item);
    }

    private void registerItemWithSpecialTypes(HabboItem item) {
        RoomSpecialTypes specialTypes = this.room.getRoomSpecialTypes();
        if (specialTypes == null) return;

        boolean wired = false;

        synchronized (specialTypes) {
            if (item instanceof WiredTickable) {
                WiredManager.registerTickable(this.room, (WiredTickable) item);
            } else if (item instanceof ICycleable) {
                specialTypes.addCycleTask((ICycleable) item);
            }

            if (item instanceof InteractionWiredTrigger) {
                specialTypes.addTrigger((InteractionWiredTrigger) item);
                wired = true;
            } else if (item instanceof InteractionWiredEffect) {
                specialTypes.addEffect((InteractionWiredEffect) item);
                wired = true;
            } else if (item instanceof InteractionWiredCondition) {
                specialTypes.addCondition((InteractionWiredCondition) item);
                wired = true;
            } else if (item instanceof InteractionWiredExtra) {
                specialTypes.addExtra((InteractionWiredExtra) item);
                wired = true;
            } else if (item instanceof InteractionBattleBanzaiTeleporter) {
                specialTypes.addBanzaiTeleporter((InteractionBattleBanzaiTeleporter) item);
            } else if (item instanceof InteractionRoller) {
                specialTypes.addRoller((InteractionRoller) item);
            } else if (item instanceof InteractionGameScoreboard) {
                specialTypes.addGameScoreboard((InteractionGameScoreboard) item);
            } else if (item instanceof InteractionGameGate) {
                specialTypes.addGameGate((InteractionGameGate) item);
            } else if (item instanceof InteractionGameTimer) {
                specialTypes.addGameTimer((InteractionGameTimer) item);
            } else if (item instanceof InteractionFreezeExitTile) {
                specialTypes.addFreezeExitTile((InteractionFreezeExitTile) item);
            } else if (item instanceof InteractionNest) {
                specialTypes.addNest((InteractionNest) item);
            } else if (item instanceof InteractionPetDrink) {
                specialTypes.addPetDrink((InteractionPetDrink) item);
            } else if (item instanceof InteractionPetFood) {
                specialTypes.addPetFood((InteractionPetFood) item);
            } else if (item instanceof InteractionPetToy) {
                specialTypes.addPetToy((InteractionPetToy) item);
            } else if (item instanceof InteractionPetTree) {
                specialTypes.addPetTree((InteractionPetTree) item);
            } else if (item instanceof InteractionMoodLight ||
                    item instanceof InteractionPyramid ||
                    item instanceof InteractionMusicDisc ||
                    item instanceof InteractionBattleBanzaiSphere ||
                    item instanceof InteractionTalkingFurniture ||
                    item instanceof InteractionWater ||
                    item instanceof InteractionWaterItem ||
                    item instanceof InteractionMuteArea ||
                    item instanceof InteractionBuildArea ||
                    item instanceof InteractionTagPole ||
                    item instanceof InteractionTagField ||
                    item instanceof InteractionJukeBox ||
                    item instanceof InteractionPetBreedingNest ||
                    item instanceof InteractionBlackHole ||
                    item instanceof InteractionWiredHighscore ||
                    item instanceof InteractionStickyPole ||
                    item instanceof WiredBlob ||
                    item instanceof InteractionTent ||
                    item instanceof InteractionSnowboardSlope ||
                    item instanceof InteractionFireworks) {
                specialTypes.addUndefined(item);
            }
        }

        invalidateWiredIf(wired);
    }

    public void removeHabboItem(int id) {
        this.removeHabboItem(this.getHabboItem(id));
    }

    public void removeHabboItem(HabboItem item) {
        if (item == null) return;

        HabboItem removed;
        synchronized (this.roomItems) {
            removed = this.roomItems.remove(item.getId());
        }

        if (removed != null) {
            synchronized (this.furniOwnerCount) {
                synchronized (this.furniOwnerNames) {
                    int count = this.furniOwnerCount.get(removed.getUserId());
                    if (count > 1) {
                        this.furniOwnerCount.put(removed.getUserId(), count - 1);
                    } else {
                        this.furniOwnerCount.remove(removed.getUserId());
                        this.furniOwnerNames.remove(removed.getUserId());
                    }
                }
            }

            this.unregisterItemFromSpecialTypes(item);
        }
    }

    private void unregisterItemFromSpecialTypes(HabboItem item) {
        RoomSpecialTypes specialTypes = this.room.getRoomSpecialTypes();
        if (specialTypes == null) return;

        boolean wired = false;

        if (item instanceof WiredTickable) {
            WiredManager.unregisterTickable(this.room, (WiredTickable) item);
        } else if (item instanceof ICycleable) {
            specialTypes.removeCycleTask((ICycleable) item);
        }

        if (item instanceof InteractionBattleBanzaiTeleporter) {
            specialTypes.removeBanzaiTeleporter((InteractionBattleBanzaiTeleporter) item);
        } else if (item instanceof InteractionWiredTrigger) {
            specialTypes.removeTrigger((InteractionWiredTrigger) item);
            wired = true;
        } else if (item instanceof InteractionWiredEffect) {
            specialTypes.removeEffect((InteractionWiredEffect) item);
            wired = true;
        } else if (item instanceof InteractionWiredCondition) {
            specialTypes.removeCondition((InteractionWiredCondition) item);
            wired = true;
        } else if (item instanceof InteractionWiredExtra) {
            specialTypes.removeExtra((InteractionWiredExtra) item);
            wired = true;
        } else if (item instanceof InteractionRoller) {
            specialTypes.removeRoller((InteractionRoller) item);
        } else if (item instanceof InteractionGameScoreboard) {
            specialTypes.removeScoreboard((InteractionGameScoreboard) item);
        } else if (item instanceof InteractionGameGate) {
            specialTypes.removeGameGate((InteractionGameGate) item);
        } else if (item instanceof InteractionGameTimer) {
            specialTypes.removeGameTimer((InteractionGameTimer) item);
        } else if (item instanceof InteractionFreezeExitTile) {
            specialTypes.removeFreezeExitTile((InteractionFreezeExitTile) item);
        } else if (item instanceof InteractionNest) {
            specialTypes.removeNest((InteractionNest) item);
        } else if (item instanceof InteractionPetDrink) {
            specialTypes.removePetDrink((InteractionPetDrink) item);
        } else if (item instanceof InteractionPetFood) {
            specialTypes.removePetFood((InteractionPetFood) item);
        } else if (item instanceof InteractionPetToy) {
            specialTypes.removePetToy((InteractionPetToy) item);
        } else if (item instanceof InteractionPetTree) {
            specialTypes.removePetTree((InteractionPetTree) item);
        } else if (item instanceof InteractionMoodLight ||
                item instanceof InteractionPyramid ||
                item instanceof InteractionMusicDisc ||
                item instanceof InteractionBattleBanzaiSphere ||
                item instanceof InteractionTalkingFurniture ||
                item instanceof InteractionWaterItem ||
                item instanceof InteractionWater ||
                item instanceof InteractionMuteArea ||
                item instanceof InteractionTagPole ||
                item instanceof InteractionTagField ||
                item instanceof InteractionJukeBox ||
                item instanceof InteractionPetBreedingNest ||
                item instanceof InteractionBlackHole ||
                item instanceof InteractionWiredHighscore ||
                item instanceof InteractionStickyPole ||
                item instanceof WiredBlob ||
                item instanceof InteractionTent ||
                item instanceof InteractionSnowboardSlope) {
            specialTypes.removeUndefined(item);
        }

        invalidateWiredIf(wired);
    }

    // =====================================================================
    // ITEM UPDATES
    // =====================================================================

    public void updateItem(HabboItem item) {
        if (this.room.isLoaded()) {
            if (item != null && item.getRoomId() == this.room.getId()) {
                if (item.getBaseItem() != null) {
                    if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
                        this.room.sendComposer(new FloorItemUpdateComposer(item).compose());
                        this.room.updateTiles(this.room.getLayout()
                                .getTilesAt(this.room.getLayout().getTile(item.getX(), item.getY()),
                                        item.getBaseItem().getWidth(), item.getBaseItem().getLength(),
                                        item.getRotation()));
                    } else if (item.getBaseItem().getType() == FurnitureType.WALL) {
                        this.room.sendComposer(new WallItemUpdateComposer(item).compose());
                    }
                }
            }
        }
    }

    public void updateItemState(HabboItem item) {
        if (!item.isLimited()) {
            this.room.sendComposer(new ItemStateComposer(item).compose());
        } else {
            this.room.sendComposer(new FloorItemUpdateComposer(item).compose());
        }

        if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
            if (this.room.getLayout() == null) {
                return;
            }

            this.room.updateTiles(this.room.getLayout()
                    .getTilesAt(this.room.getLayout().getTile(item.getX(), item.getY()),
                            item.getBaseItem().getWidth(), item.getBaseItem().getLength(),
                            item.getRotation()));

            if (item instanceof InteractionMultiHeight) {
                ((InteractionMultiHeight) item).updateUnitsOnItem(this.room);
            }
        }
    }

    // =====================================================================
    // FURNITURE OWNER MANAGEMENT
    // =====================================================================

    public TIntObjectMap<String> getFurniOwnerNames() {
        return this.furniOwnerNames;
    }

    public TIntIntMap getFurniOwnerCount() {
        return this.furniOwnerCount;
    }

    public String getFurniOwnerName(int oduserId) {
        return this.furniOwnerNames.get(oduserId);
    }

    public int getUserFurniCount(int userId) {
        return this.furniOwnerCount.get(userId);
    }

    public int getUserUniqueFurniCount(int userId) {
        THashSet<Item> items = new THashSet<>();
        for (HabboItem item : this.roomItems.valueCollection()) {
            if (!items.contains(item.getBaseItem()) && item.getUserId() == userId) {
                items.add(item.getBaseItem());
            }
        }
        return items.size();
    }

    // =====================================================================
    // PICKUP AND EJECT
    // =====================================================================

    public void pickUpItem(HabboItem item, Habbo picker) {
        if (item == null) return;

        if (Emulator.getPluginManager().isRegistered(FurniturePickedUpEvent.class, true)) {
            FurniturePickedUpEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurniturePickedUpEvent(item, picker));

            if (event.isCancelled()) return;
        }

        this.removeHabboItem(item);
        item.onPickUp(this.room);
        item.setRoomId(0);
        item.needsUpdate(true);

        if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
            this.room.sendComposer(new RemoveFloorItemComposer(item).compose());

            THashSet<RoomTile> updatedTiles = this.room.getLayout().getTilesAt(
                    this.room.getLayout().getTile(item.getX(), item.getY()),
                    item.getBaseItem().getWidth(),
                    item.getBaseItem().getLength(),
                    item.getRotation());
            this.room.updateTiles(updatedTiles);

            for (RoomTile tile : updatedTiles) {
                this.room.updateHabbosAt(tile.x, tile.y);
                this.room.updateBotsAt(tile.x, tile.y);
            }
        } else if (item.getBaseItem().getType() == FurnitureType.WALL) {
            this.room.sendComposer(new RemoveWallItemComposer(item).compose());
        }

        Emulator.getThreading().run(item);
    }

    public void ejectUserFurni(int userId) {
        THashSet<HabboItem> items = new THashSet<>();

        forEachRoomItem(i -> {
            if (i.getUserId() == userId) {
                items.add(i);
                i.setRoomId(0);
            }
        });

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
        if (habbo != null) {
            habbo.getInventory().getItemsComponent().addItems(items);
            habbo.getClient().sendResponse(new AddHabboItemComposer(items));
        }

        for (HabboItem i : items) {
            this.pickUpItem(i, null);
        }
    }

    public void ejectUserItem(HabboItem item) {
        this.pickUpItem(item, null);
    }

    public void ejectAll() {
        this.ejectAll(null);
    }

    public void ejectAll(Habbo habbo) {
        THashMap<Integer, THashSet<HabboItem>> userItemsMap = new THashMap<>();

        forEachRoomItem(i -> {
            if (habbo != null && i.getUserId() == habbo.getHabboInfo().getId()) return;
            if (i instanceof InteractionPostIt) return;
            userItemsMap.computeIfAbsent(i.getUserId(), k -> new THashSet<>()).add(i);
        });

        for (Map.Entry<Integer, THashSet<HabboItem>> entrySet : userItemsMap.entrySet()) {
            for (HabboItem i : entrySet.getValue()) {
                this.pickUpItem(i, null);
            }

            Habbo user = Emulator.getGameEnvironment().getHabboManager().getHabbo(entrySet.getKey());
            if (user != null) {
                user.getInventory().getItemsComponent().addItems(entrySet.getValue());
                user.getClient().sendResponse(new AddHabboItemComposer(entrySet.getValue()));
            }
        }
    }

    // =====================================================================
    // LOCKED TILES
    // =====================================================================

    public THashSet<RoomTile> getLockedTiles() {
        THashSet<RoomTile> lockedTiles = new THashSet<>();

        forEachRoomItem(item -> {
            if (item.getBaseItem().getType() != FurnitureType.FLOOR) return;

            boolean found = false;
            for (RoomTile tile : lockedTiles) {
                if (tile.x == item.getX() && tile.y == item.getY()) {
                    found = true;
                    break;
                }
            }
            if (found) return;

            if (item.getRotation() == 0 || item.getRotation() == 4) {
                for (short y = 0; y < item.getBaseItem().getLength(); y++) {
                    for (short x = 0; x < item.getBaseItem().getWidth(); x++) {
                        RoomTile tile = this.room.getLayout().getTile(
                                (short) (item.getX() + x), (short) (item.getY() + y));
                        if (tile != null) lockedTiles.add(tile);
                    }
                }
            } else {
                for (short y = 0; y < item.getBaseItem().getWidth(); y++) {
                    for (short x = 0; x < item.getBaseItem().getLength(); x++) {
                        RoomTile tile = this.room.getLayout().getTile(
                                (short) (item.getX() + x), (short) (item.getY() + y));
                        if (tile != null) lockedTiles.add(tile);
                    }
                }
            }
        });

        return lockedTiles;
    }

    // =====================================================================
    // DISPOSAL
    // =====================================================================

    public void saveAllPendingItems() {
        synchronized (this.roomItems) {
            TIntObjectIterator<HabboItem> iterator = this.roomItems.iterator();

            for (int i = this.roomItems.size(); i-- > 0; ) {
                try {
                    iterator.advance();
                    if (iterator.value().needsUpdate()) {
                        iterator.value().run();
                    }
                } catch (java.util.NoSuchElementException e) {
                    break;
                }
            }
        }
    }

    public void clear() {
        synchronized (this.roomItems) {
            this.roomItems.clear();
        }
        synchronized (this.furniOwnerCount) {
            this.furniOwnerCount.clear();
        }
        synchronized (this.furniOwnerNames) {
            this.furniOwnerNames.clear();
        }
        this.tileCache.clear();
    }

    public void dispose() {
        this.clear();
    }

    // =====================================================================
    // FURNITURE PLACEMENT
    // =====================================================================

    public boolean hasObjectTypeAt(Class<?> type, int x, int y) {
        THashSet<HabboItem> items = this.getItemsAt(x, y);
        for (HabboItem item : items) {
            if (item.getClass() == type) return true;
        }
        return false;
    }

    public FurnitureMovementError canPlaceFurnitureAt(HabboItem item, Habbo habbo, RoomTile tile, int rotation) {
        if (this.itemCount() >= Room.MAXIMUM_FURNI) return FurnitureMovementError.MAX_ITEMS;
        if (tile == null || tile.state == RoomTileState.INVALID) return FurnitureMovementError.INVALID_MOVE;

        rotation %= 8;
        if (this.room.hasRights(habbo) || this.room.getGuildRightLevel(habbo)
                .isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS) || habbo.hasPermission(
                Permission.ACC_MOVEROTATE)) {
            return FurnitureMovementError.NONE;
        }

        if (habbo.getHabboStats().isRentingSpace()) {
            HabboItem rentSpace = this.getHabboItem(habbo.getHabboStats().rentedItemId);
            if (rentSpace != null) {
                if (!RoomLayout.squareInSquare(RoomLayout.getRectangle(rentSpace.getX(), rentSpace.getY(),
                                rentSpace.getBaseItem().getWidth(), rentSpace.getBaseItem().getLength(),
                                rentSpace.getRotation()),
                        RoomLayout.getRectangle(tile.x, tile.y, item.getBaseItem().getWidth(),
                                item.getBaseItem().getLength(), rotation))) {
                    return FurnitureMovementError.NO_RIGHTS;
                } else {
                    return FurnitureMovementError.NONE;
                }
            }
        }

        for (HabboItem area : this.room.getRoomSpecialTypes().getItemsOfType(InteractionBuildArea.class)) {
            if (((InteractionBuildArea) area).inSquare(tile) && ((InteractionBuildArea) area).isBuilder(
                    habbo.getHabboInfo().getUsername())) {
                return FurnitureMovementError.NONE;
            }
        }

        return FurnitureMovementError.NO_RIGHTS;
    }

    public FurnitureMovementError furnitureFitsAt(RoomTile tile, HabboItem item, int rotation) {
        return furnitureFitsAt(tile, item, rotation, true);
    }

    public FurnitureMovementError furnitureFitsAt(RoomTile tile, HabboItem item, int rotation, boolean checkForUnits) {
        RoomLayout layout = this.room.getLayout();
        if (!layout.fitsOnMap(tile, item.getBaseItem().getWidth(), item.getBaseItem().getLength(), rotation)) {
            return FurnitureMovementError.INVALID_MOVE;
        }

        if (item instanceof InteractionStackHelper || item instanceof InteractionTileWalkMagic) {
            return FurnitureMovementError.NONE;
        }

        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(tile, item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(), rotation);
        for (RoomTile t : occupiedTiles) {
            if (t.state == RoomTileState.INVALID) {
                return FurnitureMovementError.INVALID_MOVE;
            }
            if (!Emulator.getConfig().getBoolean("wired.place.under", false) || (
                    Emulator.getConfig().getBoolean("wired.place.under", false) && !item.isWalkable()
                            && !item.getBaseItem().allowSit() && !item.getBaseItem().allowLay())) {
                if (checkForUnits && this.room.hasHabbosAt(t.x, t.y)) return FurnitureMovementError.TILE_HAS_HABBOS;
                if (checkForUnits && this.room.hasBotsAt(t.x, t.y)) return FurnitureMovementError.TILE_HAS_BOTS;
                if (checkForUnits && this.room.hasPetsAt(t.x, t.y)) return FurnitureMovementError.TILE_HAS_PETS;
            }
        }

        List<Pair<RoomTile, THashSet<HabboItem>>> tileFurniList = new ArrayList<>();
        for (RoomTile t : occupiedTiles) {
            tileFurniList.add(Pair.create(t, this.getItemsAt(t)));

            HabboItem topItem = this.getTopItemAt(t.x, t.y, item);
            if (topItem != null && !topItem.getBaseItem().allowStack() && !t.getAllowStack()) {
                return FurnitureMovementError.CANT_STACK;
            }
        }

        if (!item.canStackAt(this.room, tileFurniList)) {
            return FurnitureMovementError.CANT_STACK;
        }

        return FurnitureMovementError.NONE;
    }

    // =====================================================================
    // PLACE FLOOR/WALL
    // =====================================================================

    public FurnitureMovementError placeFloorFurniAt(HabboItem item, RoomTile tile, int rotation, Habbo owner) {
        boolean pluginHelper = false;
        if (Emulator.getPluginManager().isRegistered(FurniturePlacedEvent.class, true)) {
            FurniturePlacedEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurniturePlacedEvent(item, owner, tile));
            if (event.isCancelled()) {
                return FurnitureMovementError.CANCEL_PLUGIN_PLACE;
            }
            pluginHelper = event.hasPluginHelper();
        }

        RoomLayout layout = this.room.getLayout();
        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(tile, item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(), rotation);

        FurnitureMovementError fits = furnitureFitsAt(tile, item, rotation);
        if (!fits.equals(FurnitureMovementError.NONE) && !pluginHelper) {
            return fits;
        }

        double height = tile.getStackHeight();
        for (RoomTile tile2 : occupiedTiles) {
            double sHeight = tile2.getStackHeight();
            if (sHeight > height) height = sHeight;
        }

        if (Emulator.getPluginManager().isRegistered(FurnitureBuildheightEvent.class, true)) {
            FurnitureBuildheightEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurnitureBuildheightEvent(item, owner, 0.00, height));
            if (event.hasChangedHeight()) {
                height = layout.getHeightAtSquare(tile.x, tile.y) + event.getUpdatedHeight();
            }
        }

        item.setZ(height);
        item.setX(tile.x);
        item.setY(tile.y);
        item.setRotation(rotation);
        if (!this.furniOwnerNames.containsKey(item.getUserId()) && owner != null) {
            this.furniOwnerNames.put(item.getUserId(), owner.getHabboInfo().getUsername());
        }

        item.needsUpdate(true);
        this.addHabboItem(item);
        item.setRoomId(this.room.getId());
        item.onPlace(this.room);
        this.room.updateTiles(occupiedTiles);
        this.room.sendComposer(
                new AddFloorItemComposer(item, this.getFurniOwnerName(item.getUserId())).compose());

        for (RoomTile t : occupiedTiles) {
            this.room.updateHabbosAt(t.x, t.y);
            this.room.updateBotsAt(t.x, t.y);
        }

        Emulator.getThreading().run(item);
        return FurnitureMovementError.NONE;
    }

    public FurnitureMovementError placeWallFurniAt(HabboItem item, String wallPosition, Habbo owner) {
        if (!(this.room.hasRights(owner) || this.room.getGuildRightLevel(owner)
                .isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS))) {
            return FurnitureMovementError.NO_RIGHTS;
        }

        if (Emulator.getPluginManager().isRegistered(FurniturePlacedEvent.class, true)) {
            Event furniturePlacedEvent = new FurniturePlacedEvent(item, owner, null);
            Emulator.getPluginManager().fireEvent(furniturePlacedEvent);
            if (furniturePlacedEvent.isCancelled()) {
                return FurnitureMovementError.CANCEL_PLUGIN_PLACE;
            }
        }

        item.setWallPosition(wallPosition);
        if (!this.furniOwnerNames.containsKey(item.getUserId()) && owner != null) {
            this.furniOwnerNames.put(item.getUserId(), owner.getHabboInfo().getUsername());
        }
        this.room.sendComposer(
                new AddWallItemComposer(item, this.getFurniOwnerName(item.getUserId())).compose());
        item.needsUpdate(true);
        this.addHabboItem(item);
        item.setRoomId(this.room.getId());
        item.onPlace(this.room);
        Emulator.getThreading().run(item);
        return FurnitureMovementError.NONE;
    }

    // =====================================================================
    // MOVE
    // =====================================================================

    /** Moves furniture to a new position with an explicit Z height. */
    public FurnitureMovementError moveFurniTo(HabboItem item, RoomTile tile, int rotation, double z, Habbo actor, boolean sendUpdates, boolean checkForUnits) {
        if (item == null || tile == null) return FurnitureMovementError.INVALID_MOVE;

        RoomLayout layout = this.room.getLayout();
        RoomTile oldLocation = layout.getTile(item.getX(), item.getY());

        boolean pluginHelper = false;
        if (Emulator.getPluginManager().isRegistered(FurnitureMovedEvent.class, true)) {
            FurnitureMovedEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurnitureMovedEvent(item, actor, oldLocation, tile));

            if (event.isCancelled()) return FurnitureMovementError.CANCEL_PLUGIN_MOVE;
            pluginHelper = event.hasPluginHelper();
        }

        rotation %= 8;

        boolean magicTile =
                item instanceof InteractionStackHelper ||
                        item instanceof InteractionTileWalkMagic;

        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(
                tile,
                item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(),
                rotation
        );

        THashSet<RoomTile> oldOccupiedTiles = layout.getTilesAt(
                layout.getTile(item.getX(), item.getY()),
                item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(),
                item.getRotation()
        );

        if (!pluginHelper) {
            FurnitureMovementError fits = furnitureFitsAt(tile, item, rotation, checkForUnits);
            if (fits != FurnitureMovementError.NONE) return fits;
        }

        int oldRotation = item.getRotation();
        if (oldRotation != rotation) {
            item.setRotation(rotation);

            if (Emulator.getPluginManager().isRegistered(FurnitureRotatedEvent.class, true)) {
                Event rotatedEvent = new FurnitureRotatedEvent(item, actor, oldRotation);
                Emulator.getPluginManager().fireEvent(rotatedEvent);

                if (rotatedEvent.isCancelled()) {
                    item.setRotation(oldRotation);
                    return FurnitureMovementError.CANCEL_PLUGIN_ROTATE;
                }
            }
        }

        // Height sanity checks
        if (z > Room.MAXIMUM_FURNI_HEIGHT) return FurnitureMovementError.CANT_STACK;
        if (z < layout.getHeightAtSquare(tile.x, tile.y)) return FurnitureMovementError.CANT_STACK;

        // Plugin height override
        z = applyBuildHeightEvent(item, actor, layout, tile.x, tile.y, z);

        // Set new position
        item.setX(tile.x);
        item.setY(tile.y);
        item.setZ(z);

        if (magicTile) {
            item.setZ(tile.z);
            item.setExtradata("" + (item.getZ() * 100));
        }

        if (item.getZ() > Room.MAXIMUM_FURNI_HEIGHT) {
            item.setZ(Room.MAXIMUM_FURNI_HEIGHT);
        }

        // Wired index update
        updateWiredLocationIfNeeded(item, oldLocation);

        // Update furniture
        item.onMove(this.room, oldLocation, tile);
        item.needsUpdate(true);
        Emulator.getThreading().run(item);

        // Shared broadcast update
        broadcastMoveUpdate(item, occupiedTiles, oldOccupiedTiles, sendUpdates);

        // "place under" behavior if enabled
        if (Emulator.getConfig().getBoolean("wired.place.under", false)) {
            THashSet<RoomTile> newOccupiedTiles = layout.getTilesAt(
                    tile,
                    item.getBaseItem().getWidth(),
                    item.getBaseItem().getLength(),
                    rotation
            );

            for (RoomTile t : newOccupiedTiles) {
                for (Habbo h : this.room.getHabbosAt(t.x, t.y)) {
                    try {
                        item.onWalkOn(h.getRoomUnit(), this.room, null);
                    } catch (Exception ignored) { }
                }
            }
        }

        return FurnitureMovementError.NONE;
    }

    public FurnitureMovementError moveFurniTo(HabboItem item, RoomTile tile, int rotation, Habbo actor) {
        return moveFurniTo(item, tile, rotation, actor, true, true);
    }

    public FurnitureMovementError moveFurniTo(HabboItem item, RoomTile tile, int rotation, Habbo actor, boolean sendUpdates) {
        return moveFurniTo(item, tile, rotation, actor, sendUpdates, true);
    }

    /**
     * Moves furniture to a new position with full options.
     * Using shared helpers and delegating the final apply/broadcast to the Z-based mover.
     */

    public FurnitureMovementError moveFurniTo(HabboItem item, RoomTile tile, int rotation, Habbo actor, boolean sendUpdates, boolean checkForUnits) {
        RoomLayout layout = this.room.getLayout();
        RoomTile oldLocation = layout.getTile(item.getX(), item.getY());

        boolean pluginHelper = false;
        if (Emulator.getPluginManager().isRegistered(FurnitureMovedEvent.class, true)) {
            FurnitureMovedEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurnitureMovedEvent(item, actor, oldLocation, tile));
            if (event.isCancelled()) {
                return FurnitureMovementError.CANCEL_PLUGIN_MOVE;
            }
            pluginHelper = event.hasPluginHelper();
        }

        boolean magicTile = item instanceof InteractionStackHelper || item instanceof InteractionTileWalkMagic;

        Optional<HabboItem> stackHelper = this.getItemsAt(tile).stream()
                .filter(i -> i instanceof InteractionStackHelper).findAny();

        // Check if it can be placed at new position
        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(tile, item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(), rotation);
        THashSet<RoomTile> newOccupiedTiles = layout.getTilesAt(tile,
                item.getBaseItem().getWidth(), item.getBaseItem().getLength(), rotation);

        HabboItem topItem = this.getTopItemAt(occupiedTiles, null);

        if (!stackHelper.isPresent() && !pluginHelper) {
            if (oldLocation != tile) {
                for (RoomTile t : occupiedTiles) {
                    HabboItem tileTopItem = this.getTopItemAt(t.x, t.y);
                    if (!magicTile && ((tileTopItem != null && tileTopItem != item ? (
                            t.state.equals(RoomTileState.INVALID) || !t.getAllowStack()
                                    || !tileTopItem.getBaseItem().allowStack())
                            : this.room.calculateTileState(t, item).equals(RoomTileState.INVALID)))) {
                        return FurnitureMovementError.CANT_STACK;
                    }

                    if (!Emulator.getConfig().getBoolean("wired.place.under", false) || (
                            Emulator.getConfig().getBoolean("wired.place.under", false) && !item.isWalkable()
                                    && !item.getBaseItem().allowSit() && !item.getBaseItem().allowLay())) {
                        if (checkForUnits) {
                            if (!magicTile && this.room.hasHabbosAt(t.x, t.y)) {
                                return FurnitureMovementError.TILE_HAS_HABBOS;
                            }
                            if (!magicTile && this.room.hasBotsAt(t.x, t.y)) {
                                return FurnitureMovementError.TILE_HAS_BOTS;
                            }
                            if (!magicTile && this.room.hasPetsAt(t.x, t.y)) {
                                return FurnitureMovementError.TILE_HAS_PETS;
                            }
                        }
                    }
                }
            }

            List<Pair<RoomTile, THashSet<HabboItem>>> tileFurniList = new ArrayList<>();
            for (RoomTile t : occupiedTiles) {
                tileFurniList.add(Pair.create(t, this.getItemsAt(t)));
            }

            if (!magicTile && !item.canStackAt(this.room, tileFurniList)) {
                return FurnitureMovementError.CANT_STACK;
            }
        }

        THashSet<RoomTile> oldOccupiedTiles = layout.getTilesAt(
                layout.getTile(item.getX(), item.getY()), item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(), item.getRotation());

        int oldRotation = item.getRotation();

        if (oldRotation != rotation) {
            item.setRotation(rotation);
            if (Emulator.getPluginManager().isRegistered(FurnitureRotatedEvent.class, true)) {
                Event furnitureRotatedEvent = new FurnitureRotatedEvent(item, actor, oldRotation);
                Emulator.getPluginManager().fireEvent(furnitureRotatedEvent);

                if (furnitureRotatedEvent.isCancelled()) {
                    item.setRotation(oldRotation);
                    return FurnitureMovementError.CANCEL_PLUGIN_ROTATE;
                }
            }

            if ((!stackHelper.isPresent() && topItem != null && topItem != item && !topItem.getBaseItem()
                    .allowStack()) || (topItem != null && topItem != item
                    && topItem.getZ() + Item.getCurrentHeight(topItem) + Item.getCurrentHeight(item)
                    > Room.MAXIMUM_FURNI_HEIGHT)) {
                item.setRotation(oldRotation);
                return FurnitureMovementError.CANT_STACK;
            }
        }

        // Compute height
        double height;

        if (stackHelper.isPresent()) {
            height = stackHelper.get().getExtradata().isEmpty() ? Double.parseDouble("0.0")
                    : (Double.parseDouble(stackHelper.get().getExtradata()) / 100);
        } else if (item == topItem) {
            height = item.getZ();
        } else if (magicTile) {
            if (topItem == null) {
                height = this.room.getStackHeight(tile.x, tile.y, false, item);
                for (RoomTile til : occupiedTiles) {
                    double sHeight = this.room.getStackHeight(til.x, til.y, false, item);
                    if (sHeight > height) height = sHeight;
                }
            } else {
                height = topItem.getZ() + topItem.getBaseItem().getHeight();
            }
        } else {
            height = this.room.getStackHeight(tile.x, tile.y, false, item);
            for (RoomTile til : occupiedTiles) {
                double sHeight = this.room.getStackHeight(til.x, til.y, false, item);
                if (sHeight > height) height = sHeight;
            }
        }

        boolean cantStack = false;
        boolean pluginHeight = false;

        if (height > Room.MAXIMUM_FURNI_HEIGHT) cantStack = true;
        if (height < layout.getHeightAtSquare(tile.x, tile.y)) cantStack = true;

        if (Emulator.getPluginManager().isRegistered(FurnitureBuildheightEvent.class, true)) {
            FurnitureBuildheightEvent event = Emulator.getPluginManager()
                    .fireEvent(new FurnitureBuildheightEvent(item, actor, 0.00, height));
            if (event.hasChangedHeight()) {
                height = layout.getHeightAtSquare(tile.x, tile.y) + event.getUpdatedHeight();
                pluginHeight = true;
            }
        }

        if (!pluginHeight && cantStack) {
            return FurnitureMovementError.CANT_STACK;
        }

        // Apply new position
        item.setX(tile.x);
        item.setY(tile.y);
        item.setZ(height);

        if (magicTile) {
            item.setZ(tile.z);
            item.setExtradata("" + item.getZ() * 100);
        }
        if (item.getZ() > Room.MAXIMUM_FURNI_HEIGHT) {
            item.setZ(Room.MAXIMUM_FURNI_HEIGHT);
        }

        // Wired location update
        updateWiredLocationIfNeeded(item, oldLocation);

        // Update furniture
        item.onMove(this.room, oldLocation, tile);
        item.needsUpdate(true);
        Emulator.getThreading().run(item);

        // Shared broadcast update
        broadcastMoveUpdate(item, occupiedTiles, oldOccupiedTiles, sendUpdates);

        if (Emulator.getConfig().getBoolean("wired.place.under", false)) {
            for (RoomTile t : newOccupiedTiles) {
                for (Habbo h : this.room.getHabbosAt(t.x, t.y)) {
                    try {
                        item.onWalkOn(h.getRoomUnit(), this.room, null);
                    } catch (Exception ignored) { }
                }
            }
        }

        return FurnitureMovementError.NONE;
    }


    public FurnitureMovementError slideFurniTo(HabboItem item, RoomTile tile, int rotation) {
        boolean magicTile = item instanceof InteractionStackHelper;

        RoomLayout layout = this.room.getLayout();

        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(tile, item.getBaseItem().getWidth(),
                item.getBaseItem().getLength(), rotation);

        List<Pair<RoomTile, THashSet<HabboItem>>> tileFurniList = new ArrayList<>();
        for (RoomTile t : occupiedTiles) {
            tileFurniList.add(Pair.create(t, this.getItemsAt(t)));
        }

        if (!magicTile && !item.canStackAt(this.room, tileFurniList)) {
            return FurnitureMovementError.CANT_STACK;
        }

        item.setRotation(rotation);

        if (magicTile) {
            item.setZ(tile.z);
            item.setExtradata("" + item.getZ() * 100);
        }
        if (item.getZ() > Room.MAXIMUM_FURNI_HEIGHT) {
            item.setZ(Room.MAXIMUM_FURNI_HEIGHT);
        }

        double offset = this.room.getStackHeight(tile.x, tile.y, false, item) - item.getZ();
        this.room.sendComposer(new FloorItemOnRollerComposer(item, null, tile, offset, this.room).compose());

        for (RoomTile t : occupiedTiles) {
            this.room.updateHabbosAt(t.x, t.y);
            this.room.updateBotsAt(t.x, t.y);
        }
        return FurnitureMovementError.NONE;
    }
}