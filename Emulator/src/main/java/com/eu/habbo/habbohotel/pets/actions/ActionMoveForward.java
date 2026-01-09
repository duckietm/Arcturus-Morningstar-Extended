package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionMoveForward extends PetAction {
    public ActionMoveForward() {
        super(null, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        if (pet.getRoom() == null || pet.getRoomUnit() == null) {
            return false;
        }
        
        RoomTile targetTile = pet.getRoom().getLayout().getTileInFront(
            pet.getRoomUnit().getCurrentLocation(), 
            pet.getRoomUnit().getBodyRotation().getValue()
        );
        
        if (targetTile != null && pet.getRoom().getLayout().tileWalkable(targetTile.x, targetTile.y)) {
            pet.getRoomUnit().setGoalLocation(targetTile);
            pet.getRoomUnit().setCanWalk(true);
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));
            return true;
        }
        
        pet.say(pet.getPetData().randomVocal(PetVocalsType.DISOBEY));
        return false;
    }
}
