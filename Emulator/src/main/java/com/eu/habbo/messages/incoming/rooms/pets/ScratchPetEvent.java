package com.eu.habbo.messages.incoming.rooms.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.MonsterplantPet;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserRespectedEvent;
import com.eu.habbo.threading.runnables.RoomUnitWalkToLocation;

import java.util.ArrayList;
import java.util.List;

public class ScratchPetEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        final int petId = this.packet.readInt();

        final Habbo habbo = this.client.getHabbo();
        if (habbo == null) {
            return;
        }

        final Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) {
            return;
        }

        final Pet pet = room.getPet(petId);
        if (pet == null) {
            this.scratchMorphedUser(habbo, room, petId);
            return;
        }

        if (habbo.getHabboStats().petRespectPointsToGive > 0
                || habbo.hasPermission(Permission.ACC_INFINITE_RESPECT)
                || pet instanceof MonsterplantPet) {

            List<Runnable> tasks = new ArrayList<>();
            tasks.add(() -> {
                pet.scratched(habbo);
                Emulator.getThreading().run(pet);
            });

            RoomTile closestTile = habbo.getRoomUnit().getClosestAdjacentTile(pet.getRoomUnit().getX(), pet.getRoomUnit().getY(), true);
            if (closestTile != null) {
                habbo.getRoomUnit().setGoalLocation(closestTile);
                Emulator.getThreading().run(new RoomUnitWalkToLocation(habbo.getRoomUnit(), closestTile, room, tasks, tasks));
            }
        }
    }

    /**
     * "Grattini" given to a user morphed with {@code :transform}: the player behind the pet
     * earns a respect point. Consumes a pet-respect point when available, else a user-respect
     * point (staff with infinite respect never pay).
     */
    private void scratchMorphedUser(Habbo habbo, Room room, int userId) {
        final Habbo target = room.getHabbo(userId);
        if (target == null || target == habbo || target.getRoomUnit() == null
                || target.getRoomUnit().getRoomUnitType() != RoomUnitType.PET) {
            return;
        }

        boolean infinite = habbo.hasPermission(Permission.ACC_INFINITE_RESPECT);
        boolean usePetPoints = habbo.getHabboStats().petRespectPointsToGive > 0;
        if (!infinite && !usePetPoints && habbo.getHabboStats().respectPointsToGive <= 0) return;

        if (Emulator.getPluginManager().isRegistered(UserRespectedEvent.class, false)
                && Emulator.getPluginManager().fireEvent(new UserRespectedEvent(target, habbo)).isCancelled()) {
            return;
        }

        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> {
            if (!infinite && usePetPoints) {
                // Habbo.respect() decrements the user pool; pay with the pet pool instead.
                habbo.getHabboStats().petRespectPointsToGive--;
                habbo.getHabboStats().respectPointsToGive++;
            }
            habbo.respect(target);
        });

        RoomTile closestTile = habbo.getRoomUnit().getClosestAdjacentTile(target.getRoomUnit().getX(), target.getRoomUnit().getY(), true);
        if (closestTile != null) {
            habbo.getRoomUnit().setGoalLocation(closestTile);
            Emulator.getThreading().run(new RoomUnitWalkToLocation(habbo.getRoomUnit(), closestTile, room, tasks, tasks));
        } else {
            tasks.forEach(Runnable::run);
        }
    }
}
