package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionRelax extends PetAction {
    public ActionRelax() {
        super(null, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        //Relax
        if (pet.getHappiness() > 75)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_HAPPY));
        else if (pet.getHappiness() < 30)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_SAD));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        pet.getRoomUnit().setStatus(RoomUnitStatus.RELAX, "0");

        return true;
    }
}
