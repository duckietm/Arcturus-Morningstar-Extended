package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetFollowHabbo;

public class ActionFollowLeft extends PetAction {
    public ActionFollowLeft() {
        super(PetTasks.FOLLOW, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        //Follow left.
        pet.clearPosture();

        Emulator.getThreading().run(new PetFollowHabbo(pet, habbo, -2));

        if (pet.getHappiness() > 75)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
