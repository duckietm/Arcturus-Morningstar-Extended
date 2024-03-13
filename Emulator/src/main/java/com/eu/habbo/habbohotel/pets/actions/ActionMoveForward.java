package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionMoveForward extends PetAction {
    public ActionMoveForward() {
        super(null, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {

        pet.getRoomUnit().setGoalLocation(pet.getRoom().getLayout().getTileInFront(pet.getRoomUnit().getCurrentLocation(), pet.getRoomUnit().getBodyRotation().getValue()));
        pet.getRoomUnit().setCanWalk(true);

        pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return false;
    }
}
