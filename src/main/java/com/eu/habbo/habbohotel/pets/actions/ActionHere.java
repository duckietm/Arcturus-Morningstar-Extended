package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionHere extends PetAction {
    public ActionHere() {
        super(PetTasks.HERE, false);

        this.statusToRemove.add(RoomUnitStatus.DEAD);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        if (pet.getRoom() == null || habbo.getRoomUnit() == null) {
            return false;
        }
        
        pet.clearPosture();
        
        // Try tile in front of habbo first
        RoomTile target = pet.getRoom().getLayout().getTileInFront(
            habbo.getRoomUnit().getCurrentLocation(), 
            habbo.getRoomUnit().getBodyRotation().getValue()
        );
        
        // If not walkable, try habbo's current tile
        if (target == null || !pet.getRoom().getLayout().tileWalkable(target.x, target.y)) {
            target = habbo.getRoomUnit().getCurrentLocation();
        }
        
        if (target != null) {
            pet.getRoomUnit().setGoalLocation(target);
            pet.getRoomUnit().setCanWalk(true);
        }

        if (pet.getHappiness() > 75)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
