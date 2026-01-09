package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionFree extends PetAction {
    public ActionFree() {
        super(PetTasks.FREE, false);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.freeCommand();
        
        if (pet.getHappiness() > 50)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_HAPPY));

        return true;
    }
}
