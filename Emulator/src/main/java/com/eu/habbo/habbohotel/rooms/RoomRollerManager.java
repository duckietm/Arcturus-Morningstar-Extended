package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionGate;
import com.eu.habbo.habbohotel.items.interactions.InteractionPyramid;
import com.eu.habbo.habbohotel.items.interactions.InteractionRoller;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.RideablePet;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUnitOnRollerComposer;
import com.eu.habbo.plugin.Event;
import com.eu.habbo.plugin.events.furniture.FurnitureRolledEvent;
import com.eu.habbo.plugin.events.users.UserRolledEvent;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages roller mechanics within a room.
 * Handles roller cycling, moving items and units on rollers.
 */
public class RoomRollerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomRollerManager.class);

    private final Room room;
    private long rollerCycle = System.currentTimeMillis();

    public RoomRollerManager(Room room) {
        this.room = room;
    }

    /**
     * Processes roller cycle for the room.
     * @param updatedUnit Set to add updated room units to
     * @param cycleTimestamp Current cycle timestamp
     * @return true if roller cycle was processed
     */
    public boolean processRollerCycle(THashSet<RoomUnit> updatedUnit, long cycleTimestamp) {
        int rollerSpeed = this.room.getRollerSpeed();
        
        if (rollerSpeed == -1) {
            return false;
        }
        
        if (this.rollerCycle < rollerSpeed) {
            this.rollerCycle++;
            return false;
        }

        this.rollerCycle = 0;

        THashSet<MessageComposer> messages = new THashSet<>();
        List<Integer> rollerFurniIds = new ArrayList<>();
        List<Integer> rolledUnitIds = new ArrayList<>();

        this.room.getRoomSpecialTypes().getRollers().forEachValue(roller -> {
            processRoller(roller, messages, rollerFurniIds, rolledUnitIds, updatedUnit);
            return true;
        });

        // Process pyramids
        int currentTime = (int) (cycleTimestamp / 1000);
        for (HabboItem pyramid : this.room.getRoomSpecialTypes().getItemsOfType(InteractionPyramid.class)) {
            if (pyramid instanceof InteractionPyramid) {
                if (((InteractionPyramid) pyramid).getNextChange() < currentTime) {
                    ((InteractionPyramid) pyramid).change(this.room);
                }
            }
        }

        return true;
    }

    /**
     * Processes a single roller and its contents.
     */
    private void processRoller(InteractionRoller roller, THashSet<MessageComposer> messages,
                               List<Integer> rollerFurniIds, List<Integer> rolledUnitIds,
                               THashSet<RoomUnit> updatedUnit) {
        
        HabboItem newRoller = null;
        RoomLayout layout = this.room.getLayout();
        
        RoomTile rollerTile = layout.getTile(roller.getX(), roller.getY());
        if (rollerTile == null) {
            return;
        }

        THashSet<HabboItem> itemsOnRoller = new THashSet<>();
        for (HabboItem item : this.room.getItemsAt(rollerTile)) {
            if (item.getZ() >= roller.getZ() + Item.getCurrentHeight(roller)) {
                itemsOnRoller.add(item);
            }
        }
        itemsOnRoller.remove(roller);

        if (!rollerTile.hasUnits() && itemsOnRoller.isEmpty()) {
            return;
        }

        RoomTile tileInFront = layout.getTileInFront(layout.getTile(roller.getX(), roller.getY()), roller.getRotation());

        if (tileInFront == null || !layout.tileExists(tileInFront.x, tileInFront.y)) {
            return;
        }

        if (tileInFront.state == RoomTileState.INVALID) {
            return;
        }

        if (!tileInFront.getAllowStack() && !(tileInFront.isWalkable()
            || tileInFront.state == RoomTileState.SIT
            || tileInFront.state == RoomTileState.LAY)) {
            return;
        }

        if (tileInFront.hasUnits()) {
            return;
        }

        THashSet<HabboItem> itemsNewTile = new THashSet<>();
        itemsNewTile.addAll(this.room.getItemsAt(tileInFront));
        itemsNewTile.removeAll(itemsOnRoller);

        List<HabboItem> toRemove = new ArrayList<>();
        for (HabboItem item : itemsOnRoller) {
            if (item.getX() != roller.getX() || item.getY() != roller.getY()
                || rollerFurniIds.contains(item.getId())) {
                toRemove.add(item);
            }
        }
        itemsOnRoller.removeAll(toRemove);
        HabboItem topItem = this.room.getTopItemAt(tileInFront.x, tileInFront.y);

        boolean allowUsers = true;
        boolean allowFurniture = true;
        boolean stackContainsRoller = false;

        for (HabboItem item : itemsNewTile) {
            if (!(item.getBaseItem().allowWalk() || item.getBaseItem().allowSit()) && !(
                item instanceof InteractionGate && item.getExtradata().equals("1"))) {
                allowUsers = false;
            }
            if (item instanceof InteractionRoller) {
                newRoller = item;
                stackContainsRoller = true;

                if ((item.getZ() != roller.getZ() || (itemsNewTile.size() > 1 && item != topItem))
                    && !InteractionRoller.NO_RULES) {
                    allowUsers = false;
                    allowFurniture = false;
                    continue;
                }

                break;
            } else {
                allowFurniture = false;
            }
        }

        if (allowFurniture) {
            allowFurniture = tileInFront.getAllowStack();
        }

        double zOffset = 0;
        if (newRoller != null) {
            if ((!itemsNewTile.isEmpty() && (itemsNewTile.size() > 1)) && !InteractionRoller.NO_RULES) {
                return;
            }
        } else {
            zOffset = -Item.getCurrentHeight(roller) + tileInFront.getStackHeight() - rollerTile.z;
        }

        // Process units on roller
        if (allowUsers) {
            processUnitsOnRoller(roller, rollerTile, tileInFront, topItem, 
                itemsOnRoller, itemsNewTile, stackContainsRoller, allowFurniture,
                zOffset, messages, rolledUnitIds, updatedUnit);
        }

        // Send unit messages
        if (!messages.isEmpty()) {
            for (MessageComposer message : messages) {
                this.room.sendComposer(message.compose());
            }
            messages.clear();
        }

        // Process furniture on roller
        if (allowFurniture || !stackContainsRoller || InteractionRoller.NO_RULES) {
            processFurnitureOnRoller(roller, itemsOnRoller, newRoller, topItem,
                tileInFront, zOffset, messages, rollerFurniIds);
        }

        // Send furniture messages
        if (!messages.isEmpty()) {
            for (MessageComposer message : messages) {
                this.room.sendComposer(message.compose());
            }
            messages.clear();
        }
    }

    /**
     * Processes units (Habbos, Pets) on a roller.
     */
    private void processUnitsOnRoller(InteractionRoller roller, RoomTile rollerTile, 
                                      RoomTile tileInFront, HabboItem topItem,
                                      THashSet<HabboItem> itemsOnRoller, 
                                      THashSet<HabboItem> itemsNewTile,
                                      boolean stackContainsRoller, boolean allowFurniture,
                                      double zOffset, THashSet<MessageComposer> messages,
                                      List<Integer> rolledUnitIds, THashSet<RoomUnit> updatedUnit) {
        
        Event roomUserRolledEvent = null;

        if (Emulator.getPluginManager().isRegistered(UserRolledEvent.class, true)) {
            roomUserRolledEvent = new UserRolledEvent(null, null, null);
        }

        ArrayList<RoomUnit> unitsOnTile = new ArrayList<>(rollerTile.getUnits());

        for (RoomUnit unit : rollerTile.getUnits()) {
            if (unit.getRoomUnitType() == RoomUnitType.PET) {
                Pet pet = this.room.getPet(unit);
                if (pet instanceof RideablePet && ((RideablePet) pet).getRider() != null) {
                    unitsOnTile.remove(unit);
                }
            }
        }

        this.room.getTallestChair(tileInFront);

        THashSet<Integer> usersRolledThisTile = new THashSet<>();

        for (RoomUnit unit : unitsOnTile) {
            if (rolledUnitIds.contains(unit.getId())) {
                continue;
            }

            if (usersRolledThisTile.size() >= Room.ROLLERS_MAXIMUM_ROLL_AVATARS) {
                break;
            }

            if (stackContainsRoller && !allowFurniture && !(topItem != null && topItem.isWalkable())) {
                continue;
            }

            if (unit.hasStatus(RoomUnitStatus.MOVE)) {
                continue;
            }

            // Prevent rolling if the unit is still in roller cooldown (prevents desync/bungie effect)
            if (!unit.canBeRolled()) {
                continue;
            }

            double newZ = unit.getZ() + zOffset;

            if (roomUserRolledEvent != null && unit.getRoomUnitType() == RoomUnitType.USER) {
                roomUserRolledEvent = new UserRolledEvent(this.room.getHabbo(unit), roller, tileInFront);
                Emulator.getPluginManager().fireEvent(roomUserRolledEvent);

                if (roomUserRolledEvent.isCancelled()) {
                    continue;
                }
            }

            // Horse riding
            boolean isRiding = false;
            if (unit.getRoomUnitType() == RoomUnitType.USER) {
                Habbo rollingHabbo = this.room.getHabbo(unit);
                if (rollingHabbo != null && rollingHabbo.getHabboInfo() != null) {
                    RideablePet riding = rollingHabbo.getHabboInfo().getRiding();
                    if (riding != null && riding.getRoomUnit() != null) {
                        RoomUnit ridingUnit = riding.getRoomUnit();
                        double petOldZ = ridingUnit.getZ();
                        double petNewZ = tileInFront.getStackHeight();
                        
                        // Update pet position immediately before composing messages to prevent desync
                        rolledUnitIds.add(ridingUnit.getId());
                        updatedUnit.remove(ridingUnit);
                        
                        // Compose and send pet roller message first
                        RoomUnitOnRollerComposer petRollerComposer = new RoomUnitOnRollerComposer(
                            ridingUnit, roller, ridingUnit.getCurrentLocation(), petOldZ, 
                            tileInFront, petNewZ, this.room);
                        messages.add(petRollerComposer);
                        
                        // Update newZ for the rider (1 unit above pet)
                        newZ = petNewZ + 1.0;
                        isRiding = true;
                    }
                }
            }

            usersRolledThisTile.add(unit.getId());
            rolledUnitIds.add(unit.getId());
            updatedUnit.remove(unit);
            
            // For riding users, use pet-relative Z values
            double riderOldZ = isRiding ? unit.getZ() : unit.getZ();
            double riderNewZ = isRiding ? newZ : (unit.getZ() + zOffset);
            messages.add(new RoomUnitOnRollerComposer(unit, roller, unit.getCurrentLocation(),
                riderOldZ, tileInFront, riderNewZ, this.room));

            if (itemsOnRoller.isEmpty()) {
                HabboItem item = this.room.getTopItemAt(tileInFront.x, tileInFront.y);

                if (item != null && itemsNewTile.contains(item) && !itemsOnRoller.contains(item)) {
                    final RoomUnit finalUnit = unit;
                    final RoomTile finalRollerTile = rollerTile;
                    Emulator.getThreading().run(() -> {
                        if (finalUnit.getGoal() == finalRollerTile) {
                            try {
                                item.onWalkOn(finalUnit, this.room, new Object[]{finalRollerTile, tileInFront});
                            } catch (Exception e) {
                                LOGGER.error("Caught exception", e);
                            }
                        }
                    }, this.room.getRollerSpeed() == 0 ? 250 : InteractionRoller.DELAY);
                }
            }

            if (unit.hasStatus(RoomUnitStatus.SIT)) {
                unit.sitUpdate = true;
            }
        }
    }

    /**
     * Processes furniture items on a roller.
     */
    private void processFurnitureOnRoller(InteractionRoller roller, THashSet<HabboItem> itemsOnRoller,
                                          HabboItem newRoller, HabboItem topItem, RoomTile tileInFront,
                                          double zOffset, THashSet<MessageComposer> messages,
                                          List<Integer> rollerFurniIds) {
        
        Event furnitureRolledEvent = null;

        if (Emulator.getPluginManager().isRegistered(FurnitureRolledEvent.class, true)) {
            furnitureRolledEvent = new FurnitureRolledEvent(null, null, null);
        }

        if (newRoller == null || topItem == newRoller) {
            List<HabboItem> sortedItems = new ArrayList<>(itemsOnRoller);
            sortedItems.sort((o1, o2) -> o1.getZ() > o2.getZ() ? -1 : 1);

            for (HabboItem item : sortedItems) {
                if (item.getX() == roller.getX() && item.getY() == roller.getY() && zOffset <= 0) {
                    if (item != roller) {
                        if (furnitureRolledEvent != null) {
                            furnitureRolledEvent = new FurnitureRolledEvent(item, roller, tileInFront);
                            Emulator.getPluginManager().fireEvent(furnitureRolledEvent);

                            if (furnitureRolledEvent.isCancelled()) {
                                continue;
                            }
                        }

                        messages.add(new FloorItemOnRollerComposer(item, roller, tileInFront, zOffset, this.room));
                        rollerFurniIds.add(item.getId());
                    }
                }
            }
        }
    }

    /**
     * Gets the current roller cycle value.
     */
    public long getRollerCycle() {
        return this.rollerCycle;
    }

    /**
     * Resets the roller cycle.
     */
    public void resetRollerCycle() {
        this.rollerCycle = 0;
    }
}
