package com.eu.habbo.habbohotel.users.inventory;

import com.eu.habbo.Emulator;
import com.eu.habbo.database.SqlQueries;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.plugin.events.inventory.InventoryItemAddedEvent;
import com.eu.habbo.plugin.events.inventory.InventoryItemRemovedEvent;
import com.eu.habbo.plugin.events.inventory.InventoryItemsAddedEvent;
import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemsComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsComponent.class);

    private final TIntObjectMap<HabboItem> items = TCollections.synchronizedMap(new TIntObjectHashMap<>());

    private final HabboInventory inventory;

    public ItemsComponent(HabboInventory inventory, Habbo habbo) {
        this.inventory = inventory;
        this.items.putAll(loadItems(habbo));
    }

    public static THashMap<Integer, HabboItem> loadItems(Habbo habbo) {
        THashMap<Integer, HabboItem> itemsList = new THashMap<>();

        try {
            SqlQueries.forEach(
                    "SELECT * FROM items WHERE room_id = ? AND user_id = ?",
                    rs -> {
                        try {
                            HabboItem item = Emulator.getGameEnvironment().getItemManager().loadHabboItem(rs);
                            if (item != null) {
                                itemsList.put(rs.getInt("id"), item);
                            } else {
                                LOGGER.error("Failed to load HabboItem: {}", rs.getInt("id"));
                            }
                        } catch (SQLException e) {
                            LOGGER.error("Caught SQL exception", e);
                        }
                    },
                    0, habbo.getHabboInfo().getId());
        } catch (SqlQueries.DataAccessException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return itemsList;
    }

    public void addItem(HabboItem item) {
        if (item == null) {
            return;
        }

        InventoryItemAddedEvent event = new InventoryItemAddedEvent(this.inventory, item);
        if (Emulator.getPluginManager().fireEvent(event).isCancelled()) {
            return;
        }

        synchronized (this.items) {
            this.items.put(event.item.getId(), event.item);
        }
    }

    public void addItems(THashSet<HabboItem> items) {
        InventoryItemsAddedEvent event = new InventoryItemsAddedEvent(this.inventory, items);
        if (Emulator.getPluginManager().fireEvent(event).isCancelled()) {
            return;
        }

        synchronized (this.items) {
            for (HabboItem item : event.items) {
                if (item == null) {
                    continue;
                }

                this.items.put(item.getId(), item);
            }
        }
    }

    public HabboItem getHabboItem(int itemId) {
        return this.items.get(Math.abs(itemId));
    }

    public HabboItem getAndRemoveHabboItem(final Item item) {
        final HabboItem[] habboItem = {null};
        synchronized (this.items) {
            this.items.forEachValue(new TObjectProcedure<HabboItem>() {
                @Override
                public boolean execute(HabboItem object) {
                    if (object.getBaseItem() == item) {
                        habboItem[0] = object;
                        return false;
                    }

                    return true;
                }
            });
        }
        this.removeHabboItem(habboItem[0]);
        return habboItem[0];
    }

    public void removeHabboItem(int itemId) {
        this.items.remove(itemId);
    }

    public void removeHabboItem(HabboItem item) {
        InventoryItemRemovedEvent event = new InventoryItemRemovedEvent(this.inventory, item);
        if (Emulator.getPluginManager().fireEvent(event).isCancelled()) {
            return;
        }

        synchronized (this.items) {
            this.items.remove(event.item.getId());
        }
    }

    public TIntObjectMap<HabboItem> getItems() {
        return this.items;
    }

    public THashSet<HabboItem> getItemsAsValueCollection() {
        THashSet<HabboItem> items = new THashSet<>();
        items.addAll(this.items.valueCollection());

        return items;
    }

    public int itemCount() {
        return this.items.size();
    }

    public void dispose() {
        synchronized (this.items) {
            if (!this.items.isEmpty()) {
                List<HabboItem> updates = new ArrayList<>();
                List<HabboItem> deletes = new ArrayList<>();
                for (HabboItem item : this.items.valueCollection()) {
                    if (item.needsDelete()) {
                        deletes.add(item);
                        item.needsUpdate(false);
                        item.needsDelete(false);
                    } else if (item.needsUpdate()) {
                        updates.add(item);
                        item.needsUpdate(false);
                    }
                }

                try {
                    if (!deletes.isEmpty()) {
                        SqlQueries.batchUpdate(
                                "DELETE FROM items WHERE id = ?",
                                deletes,
                                (ps, item) -> ps.setInt(1, item.getId()));
                    }
                    if (!updates.isEmpty()) {
                        SqlQueries.batchUpdate(
                                "UPDATE items SET user_id = ?, room_id = ?, wall_pos = ?, x = ?, y = ?, z = ?, rot = ?, extra_data = ?, limited_data = ? WHERE id = ?",
                                updates,
                                (ps, item) -> {
                                    ps.setInt(1, item.getUserId());
                                    ps.setInt(2, item.getRoomId());
                                    ps.setString(3, item.getWallPosition());
                                    ps.setInt(4, item.getX());
                                    ps.setInt(5, item.getY());
                                    ps.setDouble(6, item.getZ());
                                    ps.setInt(7, item.getRotation());
                                    ps.setString(8, item.getExtradata());
                                    ps.setString(9, item.getLimitedStack() + ":" + item.getLimitedSells());
                                    ps.setInt(10, item.getId());
                                });
                    }
                } catch (SqlQueries.DataAccessException e) {
                    LOGGER.error("Caught SQL exception during batch item save", e);
                }
            }

            this.items.clear();
        }
    }
}
