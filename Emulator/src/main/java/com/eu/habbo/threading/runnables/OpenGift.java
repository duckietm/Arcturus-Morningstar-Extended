package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.interactions.InteractionGift;
import com.eu.habbo.habbohotel.rooms.FurnitureMovementError;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryUpdateItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.PresentItemOpenedComposer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenGift implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenGift.class);

    private final HabboItem item;
    private final Habbo habbo;
    private final Room room;

    public OpenGift(HabboItem item, Habbo habbo, Room room) {
        this.item = item;
        this.habbo = habbo;
        this.room = room;
    }

    @Override
    public void run() {
        try {
            HabboItem inside = null;

            Collection<HabboItem> items = ((InteractionGift) this.item).loadItems();
            for (HabboItem i : items) {
                if (inside == null) inside = i;

                i.setUserId(this.habbo.getHabboInfo().getId());
                i.needsUpdate(true);
                i.run();
            }

            if (inside != null) inside.setFromGift(true);

            // Habbo behaviour: a wrapped FLOOR furni takes the wrapper's tile once the box is gone.
            // Everything else (wall items, badges, pets, bots, extra bundle items) goes to the inventory.
            // The wrapper removal is delayed for the official present_wrap explosion, so the placement
            // has to run after it - a single task does both, in order.
            final HabboItem placeCandidate =
                    (inside != null && inside.getBaseItem().getType() == FurnitureType.FLOOR && this.room.getLayout() != null)
                            ? inside
                            : null;
            final List<HabboItem> inventoryItems = new ArrayList<>(items);
            if (placeCandidate != null) inventoryItems.remove(placeCandidate);

            this.habbo.getInventory().getItemsComponent().addItems(inventoryItems);

            RoomTile tile = this.room.getLayout().getTile(this.item.getX(), this.item.getY());
            if (tile != null) {
                this.room.updateTile(tile);
            }

            Emulator.getThreading().runPersistence(new QueryDeleteHabboItem(this.item.getId()));

            final boolean delayed = this.item.getBaseItem().getName().contains("present_wrap");
            final int wrapperX = this.item.getX();
            final int wrapperY = this.item.getY();
            final int wrapperRotation = this.item.getRotation();
            final HabboItem revealed = inside;
            Emulator.getThreading()
                    .run(
                            () -> {
                                new RemoveFloorItemTask(this.room, this.item).run();

                                boolean placedInRoom = false;
                                if (placeCandidate != null) {
                                    placedInRoom = this.placeInRoom(placeCandidate, wrapperX, wrapperY, wrapperRotation);

                                    if (!placedInRoom) {
                                        this.habbo.getInventory().getItemsComponent().addItem(placeCandidate);
                                        this.habbo.getClient().sendResponse(new AddHabboItemComposer(placeCandidate));
                                        this.habbo.getClient().sendResponse(new InventoryRefreshComposer());
                                    }
                                }

                                if (revealed != null) {
                                    if (!placedInRoom) this.habbo.getClient().sendResponse(new InventoryUpdateItemComposer(revealed));
                                    this.habbo.getClient().sendResponse(new PresentItemOpenedComposer(revealed, "", placedInRoom));
                                }
                            },
                            delayed ? 5000 : 0);

            this.habbo.getClient().sendResponse(new InventoryRefreshComposer());

            Map<AddHabboItemComposer.AddHabboItemCategory, List<Integer>> unseenItems = new HashMap<>();

            for (HabboItem item : inventoryItems) {
                switch (item.getBaseItem().getType()) {
                    case WALL:
                    case FLOOR:
                        if (!unseenItems.containsKey(AddHabboItemComposer.AddHabboItemCategory.OWNED_FURNI))
                            unseenItems.put(AddHabboItemComposer.AddHabboItemCategory.OWNED_FURNI, new ArrayList<>());

                        unseenItems
                                .get(AddHabboItemComposer.AddHabboItemCategory.OWNED_FURNI)
                                .add(item.getGiftAdjustedId());

                        break;

                    case BADGE:
                        if (!unseenItems.containsKey(AddHabboItemComposer.AddHabboItemCategory.BADGE))
                            unseenItems.put(AddHabboItemComposer.AddHabboItemCategory.BADGE, new ArrayList<>());

                        unseenItems
                                .get(AddHabboItemComposer.AddHabboItemCategory.BADGE)
                                .add(item.getId()); // badges cannot be placed so no need for gift adjusted ID
                        break;

                    case PET:
                        if (!unseenItems.containsKey(AddHabboItemComposer.AddHabboItemCategory.PET))
                            unseenItems.put(AddHabboItemComposer.AddHabboItemCategory.PET, new ArrayList<>());

                        unseenItems
                                .get(AddHabboItemComposer.AddHabboItemCategory.PET)
                                .add(item.getGiftAdjustedId());
                        break;

                    case ROBOT:
                        if (!unseenItems.containsKey(AddHabboItemComposer.AddHabboItemCategory.BOT))
                            unseenItems.put(AddHabboItemComposer.AddHabboItemCategory.BOT, new ArrayList<>());

                        unseenItems
                                .get(AddHabboItemComposer.AddHabboItemCategory.BOT)
                                .add(item.getGiftAdjustedId());
                        break;

                    default:
                        break;
                }
            }

            if (!unseenItems.isEmpty()) this.habbo.getClient().sendResponse(new AddHabboItemComposer(unseenItems));
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    /** Puts the unwrapped furni where the box stood; false when the tile no longer accepts it. */
    private boolean placeInRoom(HabboItem furni, int x, int y, int rotation) {
        try {
            if (this.room.getLayout() == null) return false;

            RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);
            if (tile == null) return false;

            furni.setUserId(this.habbo.getHabboInfo().getId());
            FurnitureMovementError error = this.room.placeFloorFurniAt(furni, tile, rotation, this.habbo);

            if (error != FurnitureMovementError.NONE) {
                LOGGER.debug("Unwrapped gift {} could not be placed in room {} ({}), sent to inventory", furni.getId(), this.room.getId(), error);
                return false;
            }

            furni.needsUpdate(true);
            Emulator.getThreading().runPersistence(furni);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to place unwrapped gift {} in room {}", furni.getId(), this.room.getId(), e);
            return false;
        }
    }
}
