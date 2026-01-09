package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.items.Item;
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
        // Check if pet is thirsty enough to want water (threshold: 35 to match InteractionPetDrink)
        if (pet.getLevelThirst() >= 35) {
            // Check if there's water available in the room before sending pet to drink
            if (pet.getRoom() != null && pet.getRoom().getRoomSpecialTypes() != null) {
                Item drinkItem = pet.findDrink();
                
                if (drinkItem != null) {
                    // Water exists - pet goes to drink
                    if (pet.getLevelThirst() > 65) {
                        pet.say(pet.getPetData().randomVocal(PetVocalsType.THIRSTY));
                    }
                    pet.drink();
                    return true;
                } else {
                    // No suitable water in room - pet complains
                    pet.say(pet.getPetData().randomVocal(PetVocalsType.THIRSTY));
                    return false;
                }
            }
            return false;
        } else {
            // Pet is not thirsty - disobeys command
            pet.say(pet.getPetData().randomVocal(PetVocalsType.DISOBEY));
            return false;
        }
    }
}
