package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.*;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanzaiRandomTeleport implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanzaiRandomTeleport.class);

    private final HabboItem initialTeleporter;
    private final HabboItem targetTeleporter;
    private final RoomUnit roomUnit;
    private final Room room;
    private RoomUserRotation newRotation;

    public BanzaiRandomTeleport(HabboItem initialTeleporter, HabboItem targetTeleporter, RoomUnit roomUnit, Room room) {
        this(initialTeleporter, targetTeleporter, roomUnit, room, getRandomRotation());
    }

    public BanzaiRandomTeleport(HabboItem initialTeleporter, HabboItem targetTeleporter, RoomUnit roomUnit, Room room, RoomUserRotation newRotation) {
        this.initialTeleporter = initialTeleporter;
        this.targetTeleporter = targetTeleporter;
        this.roomUnit = roomUnit;
        this.room = room;
        this.newRotation = newRotation;
    }

    private static RoomUserRotation getRandomRotation() {
        RoomUserRotation[] rotations = RoomUserRotation.values();
        return rotations[Emulator.getRandom().nextInt(rotations.length)];
    }

    @Override
    public void run() {
        if (roomUnit == null || room == null || roomUnit.getCurrentLocation() == null) {
            LOGGER.warn("RoomUnit or Room reference is null, teleport aborted.");
            return;
        }

        final RoomTile teleporterTile = room.getLayout().getTile(initialTeleporter.getX(), initialTeleporter.getY());
        final RoomTile newLocation = room.getLayout().getTile(targetTeleporter.getX(), targetTeleporter.getY());

        // Determine if the user is riding a pet
        final boolean isRiding = isUserRiding();
        final Habbo rider = isRiding ? room.getHabbo(roomUnit) : null;
        final RoomUnit petUnit = isRiding && rider != null ? rider.getHabboInfo().getRiding().getRoomUnit() : null;

        // Move the pet onto the teleport tile before teleporting
        if (petUnit != null) {
            // Temporarily override the pet's movement logic to force it onto the teleport tile
            petUnit.setCanWalk(true); // Ensure the pet can walk
            petUnit.setGoalLocation(teleporterTile); // Set the goal location
            petUnit.setCurrentLocation(teleporterTile); // Force the pet to the teleport tile
            petUnit.setZ(teleporterTile.getStackHeight()); // Set the correct Z-height

            // Ensure both pet and rider face the same direction
            roomUnit.setRotation(this.newRotation);
            petUnit.setRotation(this.newRotation);

            // Get correct Z-height
            final double baseZ = targetTeleporter.getZ();
            final double finalPetZ = baseZ;
            final double finalRiderZ = baseZ + 1; // Rider stays above pet if riding

            // Delay to ensure the pet has reached the teleport tile
            Emulator.getThreading().run(() -> {
                room.teleportRoomUnitToLocation(petUnit, newLocation.x, newLocation.y, finalPetZ);
                petUnit.setZ(finalPetZ);

                room.teleportRoomUnitToLocation(roomUnit, newLocation.x, newLocation.y, finalRiderZ);
                roomUnit.setZ(finalRiderZ);

                // Synchronize rotations after teleportation
                petUnit.setRotation(roomUnit.getBodyRotation());
                petUnit.setBodyRotation(roomUnit.getBodyRotation());
                petUnit.setHeadRotation(roomUnit.getHeadRotation());

                // Re-enable walking after teleportation
                enableWalkingAndUpdateTeleporters(roomUnit, petUnit, 650);
            }, 1000); // Increased delay to ensure pet reaches the teleport tile
        } else {
            // If not riding, proceed with teleportation for the rider only
            roomUnit.setRotation(this.newRotation);

            final double baseZ = targetTeleporter.getZ();
            final double finalRiderZ = baseZ;

            Emulator.getThreading().run(() -> {
                room.teleportRoomUnitToLocation(roomUnit, newLocation.x, newLocation.y, finalRiderZ);
                roomUnit.setZ(finalRiderZ);

                enableWalkingAndUpdateTeleporters(roomUnit, null, 650);
            }, 700); // Standard delay for non-ridden teleportation
        }
    }

    private boolean isUserRiding() {
        if (roomUnit.getRoomUnitType() != RoomUnitType.USER) {
            return false;
        }
        Habbo habbo = room.getHabbo(roomUnit);
        return habbo != null && habbo.getHabboInfo().getRiding() != null;
    }

    private void enableWalkingAndUpdateTeleporters(RoomUnit roomUnit, RoomUnit petUnit, int delay) {
        Emulator.getThreading().run(() -> {
            roomUnit.setCanWalk(true);
            if (petUnit != null) {
                petUnit.setCanWalk(true);
            }

            // Update teleporter states
            if ("1".equals(initialTeleporter.getExtradata())) {
                initialTeleporter.setExtradata("0");
                room.updateItemState(initialTeleporter);
            }

            if ("1".equals(targetTeleporter.getExtradata())) {
                targetTeleporter.setExtradata("0");
                room.updateItemState(targetTeleporter);
            }
        }, delay);
    }
}