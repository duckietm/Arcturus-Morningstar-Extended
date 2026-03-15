package com.eu.habbo.plugin.events.inventory;

import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.set.hash.THashSet;

public class InventoryItemsAddedEvent extends InventoryEvent {
    public final THashSet<HabboItem> items;

    public InventoryItemsAddedEvent(HabboInventory inventory, THashSet<HabboItem> items) {
        super(inventory);
        this.items = items;
    }
}
