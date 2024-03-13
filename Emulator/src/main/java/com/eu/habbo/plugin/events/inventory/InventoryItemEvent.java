package com.eu.habbo.plugin.events.inventory;

import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.habbohotel.users.HabboItem;

public class InventoryItemEvent extends InventoryEvent {
    public HabboItem item;

    public InventoryItemEvent(HabboInventory inventory, HabboItem item) {
        super(inventory);

        this.item = item;
    }
}
