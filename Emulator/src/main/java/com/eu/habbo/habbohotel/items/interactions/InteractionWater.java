package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import java.awt.Rectangle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.util.Pair;

public class InteractionWater extends InteractionDefault {

    private static final String DEEP_WATER_NAME = "bw_water_2";

    private final boolean isDeepWater;
    private boolean isInRoom;

    public InteractionWater(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.isDeepWater = baseItem.getName().equalsIgnoreCase(DEEP_WATER_NAME);
        this.isInRoom = this.getRoomId() != 0;
    }

    public InteractionWater(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.isDeepWater = false;
        this.isInRoom = this.getRoomId() != 0;
    }

    @Override
    public void onMove(Room room, RoomTile oldLocation, RoomTile newLocation) {
        super.onMove(room, oldLocation, newLocation);
        this.updateWaters(room, oldLocation);
    }

    @Override
    public void onPickUp(Room room) {
        this.isInRoom = false;
        this.updateWaters(room, null);

        Object[] empty = new Object[] {};
        for (Habbo habbo : room.getHabbosOnItem(this)) {
            try {
                this.onWalkOff(habbo.getRoomUnit(), room, empty);
            } catch (Exception e) {

            }
        }

        for (Bot bot : room.getBotsOnItem(this)) {
            try {
                this.onWalkOff(bot.getRoomUnit(), room, empty);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onPlace(Room room) {
        this.isInRoom = true;
        this.updateWaters(room, null);
        super.onPlace(room);
    }

    @Override
    public void onWalkOn(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOn(roomUnit, room, objects);

        Pet pet = room.getPet(roomUnit);

        if (pet == null) return;

        if (!pet.getRoomUnit().hasStatus(RoomUnitStatus.SWIM) && pet.getPetData().canSwim) {
            pet.getRoomUnit().setStatus(RoomUnitStatus.SWIM, "");
        }
    }

    @Override
    public void onWalkOff(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {
        super.onWalkOff(roomUnit, room, objects);

        Pet pet = room.getPet(roomUnit);

        if (pet != null) {
            pet.getRoomUnit().removeStatus(RoomUnitStatus.SWIM);
            return;
        }

        if (roomUnit == null) return;

        // Out of the water, no swim effect. InteractionDefault decides this by comparing effect ids with
        // whatever furni happens to be on the destination tile, which leaves the effect on in more cases
        // than it should; for water the question is simply whether the tile still has water on it.
        RoomTile destination = null;

        if (objects != null && objects.length > 0 && objects[0] instanceof RoomTile) {
            destination = (RoomTile) objects[0];
        } else if (room.getLayout() != null) {
            destination = room.getLayout().getTile(roomUnit.getX(), roomUnit.getY());
        }

        if (destination != null && isStandingInWater(room, destination)) {
            return;
        }

        // Only the effect this water granted: someone who walked in wearing their own keeps it.
        int granted = roomUnit.getEffectId();

        if (granted > 0
                && (granted == this.getBaseItem().getEffectM() || granted == this.getBaseItem().getEffectF())) {
            room.giveEffect(roomUnit, 0, -1);
        }
    }

    /**
     * Whether a unit standing on {@code tile} is in the water rather than on something floating above it.
     *
     * <p>The tile having water on it is not the question - a platform, a raft or a jetty all sit on water
     * and lift whoever stands on them clear of it. What matters is the height the unit ends up at: the
     * surface of the tile, which is the top of its highest item. That surface being no higher than the
     * water's own means the unit is in it; a flat overlay such as fog or leaves (stack height 0) therefore
     * still counts as water, while anything that raises the surface does not.
     */
    private static boolean isStandingInWater(Room room, RoomTile tile) {
        double surface = room.getStackHeight(tile.x, tile.y, false);

        for (HabboItem item : room.getItemsAt(tile)) {
            if (!(item instanceof InteractionWater water)) continue;
            if (!water.isInRoom) continue;

            double waterSurface = water.getZ() + Item.getCurrentHeight(water);

            // The tolerance absorbs the 1e-6 the emulator gives a flat furni so that stacking still
            // advances, which would otherwise put a rug on the water a hair above it.
            if (surface <= waterSurface + 0.01) return true;
        }

        return false;
    }

    @Override
    public boolean allowWiredResetState() {
        return false;
    }

    @Override
    public boolean canToggle(Habbo habbo, Room room) {
        return false;
    }

    @Override
    public boolean canStackAt(Room room, List<Pair<RoomTile, Set<HabboItem>>> itemsAtLocation) {
        for (Pair<RoomTile, Set<HabboItem>> set : itemsAtLocation) {
            for (HabboItem item : set.getValue()) {
                if (!(item instanceof InteractionWater)) {
                    return false;
                }
            }
        }

        return super.canStackAt(room, itemsAtLocation);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        if (!super.canWalkOn(roomUnit, room, objects)) return false;

        Pet pet = room.getPet(roomUnit);

        return pet == null || pet.getPetData().canSwim;
    }

    private void updateWaters(Room room, RoomTile oldLocation) {
        // Update ourself.
        this.updateWater(room);

        // Find targets containing furni to update.
        Rectangle target = this.getRectangle(1, 1);
        Rectangle targetOld = null;

        if (oldLocation != null) {
            targetOld = RoomLayout.getRectangle(
                    oldLocation.x - 1,
                    oldLocation.y - 1,
                    this.getBaseItem().getWidth() + 2,
                    this.getBaseItem().getLength() + 2,
                    this.getRotation());
        }

        // Update neighbouring water.
        for (HabboItem item : room.getRoomSpecialTypes().getItemsOfType(InteractionWater.class)) {
            // We already updated ourself.
            if (item == this) {
                continue;
            }

            // Check if found water furni is touching or intersecting our water furni.
            // Check the same for the old location
            Rectangle itemRectangle = item.getRectangle();

            if (target.intersects(itemRectangle) || (targetOld != null && targetOld.intersects(itemRectangle))) {
                ((InteractionWater) item).updateWater(room);
            }
        }

        // Update water items we might have missed in the old location.
        if (targetOld != null) {
            for (HabboItem item : room.getRoomSpecialTypes().getItemsOfType(InteractionWaterItem.class)) {
                if (targetOld.intersects(item.getRectangle())) {
                    ((InteractionWaterItem) item).update();
                }
            }
        }
    }

    private void updateWater(Room room) {
        Rectangle target = this.getRectangle();

        // Only update water item furnis that are intersecting with us.
        for (HabboItem item : room.getRoomSpecialTypes().getItemsOfType(InteractionWaterItem.class)) {
            if (target.intersects(item.getRectangle())) {
                ((InteractionWaterItem) item).update();
            }
        }

        // Prepare bits for cutting off water.
        byte _1 = 0;
        byte _2 = 0;
        byte _3 = 0;
        byte _4 = 0;
        byte _5 = 0;
        byte _6 = 0;
        byte _7 = 0;
        byte _8 = 0;
        byte _9 = 0;
        byte _10 = 0;
        byte _11 = 0;
        byte _12 = 0;

        // Check if we are touching a water tile.
        if (this.isValidForMask(room, this.getX() - 1, this.getY() - 1, this.getZ(), true)) {
            _1 = 1;
        }
        if (this.isValidForMask(room, this.getX(), this.getY() - 1, this.getZ())) {
            _2 = 1;
        }
        if (this.isValidForMask(room, this.getX() + 1, this.getY() - 1, this.getZ())) {
            _3 = 1;
        }
        if (this.isValidForMask(room, this.getX() + 2, this.getY() - 1, this.getZ(), true)) {
            _4 = 1;
        }
        if (this.isValidForMask(room, this.getX() - 1, this.getY(), this.getZ())) {
            _5 = 1;
        }
        if (this.isValidForMask(room, this.getX() + 2, this.getY(), this.getZ())) {
            _6 = 1;
        }
        if (this.isValidForMask(room, this.getX() - 1, this.getY() + 1, this.getZ())) {
            _7 = 1;
        }
        if (this.isValidForMask(room, this.getX() + 2, this.getY() + 1, this.getZ())) {
            _8 = 1;
        }
        if (this.isValidForMask(room, this.getX() - 1, this.getY() + 2, this.getZ(), true)) {
            _9 = 1;
        }
        if (this.isValidForMask(room, this.getX(), this.getY() + 2, this.getZ())) {
            _10 = 1;
        }
        if (this.isValidForMask(room, this.getX() + 1, this.getY() + 2, this.getZ())) {
            _11 = 1;
        }
        if (this.isValidForMask(room, this.getX() + 2, this.getY() + 2, this.getZ(), true)) {
            _12 = 1;
        }

        // Check if we are touching invalid tiles.
        // if (_1  == 0 && room.getLayout().isVoidTile((short)(this.getX() -1), (short) (this.getY() -1))) _1  = 1;
        if (_2 == 0 && room.getLayout().isVoidTile(this.getX(), (short) (this.getY() - 1))) _2 = 1;
        if (_3 == 0 && room.getLayout().isVoidTile((short) (this.getX() + 1), (short) (this.getY() - 1))) _3 = 1;
        // if (_4  == 0 && room.getLayout().isVoidTile((short) (this.getX() + 2), (short) (this.getY() - 1))) _4  = 1;
        if (_5 == 0 && room.getLayout().isVoidTile((short) (this.getX() - 1), this.getY())) _5 = 1;
        if (_6 == 0 && room.getLayout().isVoidTile((short) (this.getX() + 2), this.getY())) _6 = 1;
        if (_7 == 0 && room.getLayout().isVoidTile((short) (this.getX() - 1), (short) (this.getY() + 1))) _7 = 1;
        if (_8 == 0 && room.getLayout().isVoidTile((short) (this.getX() + 2), (short) (this.getY() + 1))) _8 = 1;
        // if (_9  == 0 && room.getLayout().isVoidTile((short)(this.getX() -1), (short) (this.getY() + 2))) _9 = 1;
        if (_10 == 0 && room.getLayout().isVoidTile(this.getX(), (short) (this.getY() + 2))) _10 = 1;
        if (_11 == 0 && room.getLayout().isVoidTile((short) (this.getX() + 1), (short) (this.getY() + 2))) _11 = 1;
        // if (_12 == 0 && room.getLayout().isVoidTile((short) (this.getX() + 2), (short) (this.getY() + 2))) _12 = 1;

        // Update water.
        int result = (_1 << 11)
                | (_2 << 10)
                | (_3 << 9)
                | (_4 << 8)
                | (_5 << 7)
                | (_6 << 6)
                | (_7 << 5)
                | (_8 << 4)
                | (_9 << 3)
                | (_10 << 2)
                | (_11 << 1)
                | _12;

        String updatedData = String.valueOf(result);

        if (!this.getExtradata().equals(updatedData)) {
            this.setExtradata(updatedData);
            this.needsUpdate(true);
            room.updateItem(this);
        }
    }

    private boolean isValidForMask(Room room, int x, int y, double z) {
        return this.isValidForMask(room, x, y, z, false);
    }

    private boolean isValidForMask(Room room, int x, int y, double z, boolean corner) {
        for (HabboItem item : room.getItemsAt(x, y, z)) {
            if (item instanceof InteractionWater) {
                InteractionWater water = (InteractionWater) item;

                // Take out picked up water from the recalculation.
                if (!water.isInRoom) {
                    continue;
                }

                // Allow:
                // - masking if both are deepwater or both not.
                // - corners too because otherwise causes ugly clipping issues.
                // This allows deepwater and normal water to look nice.
                if (corner && !this.isDeepWater || water.isDeepWater == this.isDeepWater) {
                    return true;
                }
            }
        }

        return false;
    }
}
