package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionFree extends PetAction {
    public ActionFree() {
        super(PetTasks.FREE, false);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.freeCommand();

        return true;
    }
}
