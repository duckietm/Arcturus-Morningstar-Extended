package com.eu.habbo.plugin.events.inventory;

import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.habbohotel.users.HabboItem;

public class InventoryItemRemovedEvent extends InventoryItemEvent {
    public InventoryItemRemovedEvent(HabboInventory inventory, HabboItem item) {
        super(inventory, item);
    }
}
