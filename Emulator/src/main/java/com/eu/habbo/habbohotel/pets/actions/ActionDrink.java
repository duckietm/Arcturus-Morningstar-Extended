package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionDrink extends PetAction {
    public ActionDrink() {
        super(null, false);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        if (pet.getLevelThirst() > 40) {
            pet.drink();

            if (pet.getLevelThirst() > 65)
                pet.say(pet.getPetData().randomVocal(PetVocalsType.THIRSTY));

            return true;
        } else {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.DISOBEY));
        }

        return false;
    }
}
