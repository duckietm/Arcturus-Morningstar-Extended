package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionSwitch extends PetAction {
    public ActionSwitch() {
        super(PetTasks.FREE, true);
        this.minimumActionDuration = 1000;
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();
        
        // Switch/toggle behavior - pet acknowledges command
        if (pet.getHappiness() > 50)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
