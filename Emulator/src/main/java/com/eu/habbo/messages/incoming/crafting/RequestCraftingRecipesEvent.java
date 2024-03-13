package com.eu.habbo.messages.incoming.crafting;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.crafting.CraftingAltar;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.crafting.CraftableProductsComposer;

public class RequestCraftingRecipesEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();
        HabboItem item = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabboItem(itemId);

        if (item != null) {
            CraftingAltar altar = Emulator.getGameEnvironment().getCraftingManager().getAltar(item.getBaseItem());

            if (altar != null) {
                this.client.sendResponse(new CraftableProductsComposer(altar.getRecipesForHabbo(this.client.getHabbo()), altar.getIngredients()));
            }
        }
    }
}
