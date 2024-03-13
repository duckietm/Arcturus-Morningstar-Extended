package com.eu.habbo.messages.incoming.inventory;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.inventory.RemoveHabboItemComposer;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItems;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class RequestInventoryItemsDelete extends MessageHandler {
    public int getRatelimit() {
        return 500;
    }

    public void handle() {
        int itemId = this.packet.readInt();
        int amount = this.packet.readInt();
        HabboItem habboItem = this.client.getHabbo().getInventory().getItemsComponent().getHabboItem(itemId);
        if (habboItem == null)
            return;
        Item item = habboItem.getBaseItem();
        if (item == null)
            return;
        if (!hasFurnitureInInventory(this.client.getHabbo(), item, Math.abs(amount)))
            return;
        final Habbo habbo = this.client.getHabbo();
        if (habbo == null)
            return;

        List<HabboItem> toRemove = IntStream.range(0, Math.abs(amount))
                .mapToObj(i -> habbo.getInventory().getItemsComponent().getAndRemoveHabboItem(item))
                .filter(removeItem -> removeItem != null)
                .collect(Collectors.toList());

        TIntObjectHashMap<HabboItem> toRemoveMap = new TIntObjectHashMap<>();
        toRemove.forEach(removeItem -> toRemoveMap.put(removeItem.getId(), removeItem));

        toRemoveMap.forEachValue(mapHabboItem -> {
            habbo.getClient().sendResponse(new RemoveHabboItemComposer(mapHabboItem.getGiftAdjustedId()));
            return true;
        });

        habbo.getClient().sendResponse(new InventoryRefreshComposer());
        Emulator.getThreading().run(new QueryDeleteHabboItems(toRemoveMap));
    }

    private boolean hasFurnitureInInventory(Habbo habbo, Item item, Integer amount) {
        long count = habbo.getInventory().getItemsComponent().getItemsAsValueCollection().stream()
                .filter(habboItem -> habboItem.getBaseItem().getId() == item.getId())
                .count();

        return count >= amount;
    }
}
