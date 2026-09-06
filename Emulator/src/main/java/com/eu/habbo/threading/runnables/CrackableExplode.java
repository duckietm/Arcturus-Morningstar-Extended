package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.CrackableReward;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionCrackable;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrackableExplode implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrackableExplode.class);
    private final Room room;
    private final InteractionCrackable habboItem;
    private final Habbo habbo;
    private final boolean toInventory;
    private final short x;
    private final short y;

    public CrackableExplode(Room room, InteractionCrackable item, Habbo habbo, boolean toInventory, short x, short y) {
        this.room = room;
        this.habboItem = item;
        this.habbo = habbo;
        this.toInventory = toInventory;

        this.x = x;
        this.y = y;
    }

    @Override
    public void run() {
        if (this.habboItem.getRoomId() == 0) {
            return;
        }

        CrackableReward rewardData = Emulator.getGameEnvironment().getItemManager().getCrackableData(this.habboItem.getBaseItem().getId());
        Item rewardItem = Emulator.getGameEnvironment().getItemManager().getCrackableReward(this.habboItem.getBaseItem().getId());
        if (rewardData == null) {
            this.habboItem.cracked = false;
            LOGGER.error("Refusing to consume crackable {} because its configuration is missing", this.habboItem.getBaseItem().getId());
            return;
        }
        if (!rewardData.prizes.isEmpty() && rewardItem == null) {
            this.habboItem.cracked = false;
            LOGGER.error("Refusing to consume crackable {} because its selected reward TypeID is missing", this.habboItem.getBaseItem().getId());
            return;
        }

        if (!this.habboItem.resetable()) {
            this.room.removeHabboItem(this.habboItem);
            this.room.sendComposer(new RemoveFloorItemComposer(this.habboItem, true).compose());
            this.habboItem.setRoomId(0);
            Emulator.getGameEnvironment().getItemManager().deleteItem(this.habboItem);
        } else {
            this.habboItem.reset(this.room);
        }

        if (rewardItem != null) {
            HabboItem newItem = Emulator.getGameEnvironment().getItemManager().createItem(this.habboItem.allowAnyone() ? this.habbo.getHabboInfo().getId() : this.habboItem.getUserId(), rewardItem, 0, 0, "");

            if (newItem != null) {
                //Add to inventory in case if isn't possible place the item or in case is wall item
                if (this.toInventory || newItem.getBaseItem().getType() == FurnitureType.WALL) {
                    this.habbo.getInventory().getItemsComponent().addItem(newItem);
                    this.habbo.getClient().sendResponse(new AddHabboItemComposer(newItem));
                    this.habbo.getClient().sendResponse(new InventoryRefreshComposer());
                } else {
                    newItem.setX(this.x);
                    newItem.setY(this.y);
                    newItem.setZ(this.room.getStackHeight(this.x, this.y, false));
                    newItem.setRoomId(this.room.getId());
                    newItem.needsUpdate(true);
                    this.room.addHabboItem(newItem);
                    this.room.updateItem(newItem);
                    this.room.sendComposer(new AddFloorItemComposer(newItem, this.room.getFurniOwnerNames().get(newItem.getUserId())).compose());
                }
            }
        }

        this.room.updateTile(this.room.getLayout().getTile(this.x, this.y));
    }
}
