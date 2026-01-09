package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionTeleport extends PetAction {
    public ActionTeleport() {
        super(PetTasks.FREE, true);
        this.minimumActionDuration = 1000;
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        // Teleport pet to a random walkable tile near owner
        if (habbo != null && habbo.getRoomUnit() != null && pet.getRoom() != null) {
            RoomTile targetTile = pet.getRoom().getLayout().getTileInFront(
                habbo.getRoomUnit().getCurrentLocation(),
                habbo.getRoomUnit().getBodyRotation().getValue()
            );
            
            if (targetTile != null && targetTile.isWalkable()) {
                pet.getRoomUnit().setLocation(targetTile);
                pet.getRoomUnit().setZ(targetTile.getStackHeight());
                pet.packetUpdate = true;
            }
        }

        if (pet.getHappiness() > 50)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
