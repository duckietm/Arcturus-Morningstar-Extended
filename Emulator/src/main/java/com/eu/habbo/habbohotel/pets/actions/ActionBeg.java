package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionBeg extends PetAction {
    public ActionBeg() {
        super(PetTasks.BEG, true);
        this.statusToSet.add(RoomUnitStatus.BEG);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();

        if (pet.getHappyness() > 90)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
