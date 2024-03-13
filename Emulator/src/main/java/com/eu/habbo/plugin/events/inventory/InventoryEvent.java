package com.eu.habbo.plugin.events.inventory;

import com.eu.habbo.habbohotel.users.HabboInventory;
import com.eu.habbo.plugin.Event;

public abstract class InventoryEvent extends Event {
    public final HabboInventory inventory;

    protected InventoryEvent(HabboInventory inventory) {
        this.inventory = inventory;
    }
}
