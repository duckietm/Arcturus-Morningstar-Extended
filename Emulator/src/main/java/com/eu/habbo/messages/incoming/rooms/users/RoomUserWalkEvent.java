package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUnitOnRollerComposer;
import com.eu.habbo.plugin.events.users.UserIdleEvent;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomUserWalkEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomUserWalkEvent.class);

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            int x = this.packet.readInt(); // Position X
            int y = this.packet.readInt(); // Position Y

            // Get Habbo object
            Habbo habbo = this.client.getHabbo();

            // Get Room Habbo object (Unique GUID?)
            RoomUnit roomUnit = this.client.getHabbo().getRoomUnit();

            // If habbo is teleporting, dont calculate a new path
            if (roomUnit.isTeleporting)
                return;

            // If habbo is being kicked dont calculate a new path
            if (roomUnit.isKicked)
                return;

            // If habbo has control (im assuming admin, do something else, but we dont care about this part here)
            if (roomUnit.getCacheable().get("control") != null) {
                habbo = (Habbo) roomUnit.getCacheable().get("control");

                if (habbo.getHabboInfo().getCurrentRoom() != this.client.getHabbo().getHabboInfo().getCurrentRoom()) {
                    habbo.getRoomUnit().getCacheable().remove("controller");
                    this.client.getHabbo().getRoomUnit().getCacheable().remove("control");
                    habbo = this.client.getHabbo();
                }
            }

            // Get room unit?
            roomUnit = habbo.getRoomUnit();

            // Get the room the habbo is in
            Room room = habbo.getHabboInfo().getCurrentRoom();

            try {
                // If our room unit is not nullptr and we are in a room and we can walk, then calculate a new path
                if (roomUnit != null && roomUnit.isInRoom() && roomUnit.canWalk()) {
                    // If we are not teleporting calcualte a new path
                    if (!roomUnit.cmdTeleport) {
                        // Don't calculate a new path if we are on a horse
                        if (habbo.getHabboInfo().getRiding() != null && habbo.getHabboInfo().getRiding().getTask() != null && habbo.getHabboInfo().getRiding().getTask().equals(PetTasks.JUMP))
                            return;

                        // Don't calulcate a new path if are already at the end position
                        if (x == roomUnit.getX() && y == roomUnit.getY())
                            return;

                        if (room == null || room.getLayout() == null)
                            return;

                        // Reset idle status
                        if (roomUnit.isIdle()) {
                            UserIdleEvent event = new UserIdleEvent(habbo, UserIdleEvent.IdleReason.WALKED, false);
                            Emulator.getPluginManager().fireEvent(event);

                            if (!event.isCancelled()) {
                                if (!event.idle) {
                                    if (roomUnit.getRoom() != null) roomUnit.getRoom().unIdle(habbo);
                                    roomUnit.resetIdleTimer();
                                }
                            }
                        }

                        // Get room height map
                        RoomTile tile = room.getLayout().getTile((short) x, (short) y);

                        // this should never happen, if it does it would be a design flaw
                        if (tile == null) {
                            return;
                        }

                        // Don't care
                        if (habbo.getRoomUnit().hasStatus(RoomUnitStatus.LAY)) {
                            if (room.getLayout().getTilesInFront(habbo.getRoomUnit().getCurrentLocation(), habbo.getRoomUnit().getBodyRotation().getValue(), 2).contains(tile))
                                return;
                        }
                        if (room.canLayAt(tile.x, tile.y)) {
                            HabboItem bed = room.getTopItemAt(tile.x, tile.y);

                            if (bed != null && bed.getBaseItem().allowLay()) {
                                RoomTile pillow = room.getLayout().getTile(bed.getX(), bed.getY());
                                switch (bed.getRotation()) {
                                    case 0:
                                    case 4:
                                        pillow = room.getLayout().getTile((short) x, bed.getY());
                                        break;
                                    case 2:
                                    case 8:
                                        pillow = room.getLayout().getTile(bed.getX(), (short) y);
                                        break;
                                }

                                if (pillow != null && room.canLayAt(pillow.x, pillow.y)) {
                                    roomUnit.setGoalLocation(pillow);
                                    return;
                                }
                            }
                        }

                        THashSet<HabboItem> items = room.getItemsAt(tile);

                        if (items.size() > 0) {
                            for (HabboItem item : items) {
                                RoomTile overriddenTile = item.getOverrideGoalTile(roomUnit, room, tile);

                                if (overriddenTile == null) {
                                    return; // null cancels the entire event
                                }

                                if (!overriddenTile.equals(tile) && overriddenTile.isWalkable()) {
                                    tile = overriddenTile;
                                    break;
                                }
                            }
                        }

                        // This is where we set the end location and begin finding a path
                        if (tile.isWalkable() || room.canSitOrLayAt(tile.x, tile.y)) {
                            if (roomUnit.getMoveBlockingTask() != null) roomUnit.getMoveBlockingTask().get();

                            roomUnit.setGoalLocation(tile);
                        }
                    } else {
                        RoomTile t = room.getLayout().getTile((short) x, (short) y);
                        room.sendComposer(new RoomUnitOnRollerComposer(roomUnit, t, room).compose());

                        if (habbo.getHabboInfo().getRiding() != null) {
                            room.sendComposer(new RoomUnitOnRollerComposer(habbo.getHabboInfo().getRiding().getRoomUnit(), t, room).compose());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Caught exception", e);
            }
        }
    }
}
