package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class InventoryUpdateItemComposer extends MessageComposer {
    private final HabboItem habboItem;

    public InventoryUpdateItemComposer(HabboItem item) {
        this.habboItem = item;
    }

    /**
     * FurniListAddOrUpdate (104): the client adds or refreshes this one item in place, no full list round trip.
     * Same layout as an InventoryItemsComposer entry - it used to be hand-written here with a category that
     * doubled up for wallpapers and an extradata path that ignored every interaction's own serializer.
     */
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.InventoryItemUpdateComposer);
        InventoryItemsComposer.serializeInventoryItem(this.response, this.habboItem);
        return this.response;
    }

    public HabboItem getHabboItem() {
        return habboItem;
    }
}
