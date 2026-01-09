package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionWagTail extends PetAction {
    public ActionWagTail() {
        super(PetTasks.FREE, true);
        this.minimumActionDuration = 2000;
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();
        pet.getRoomUnit().setStatus(RoomUnitStatus.WAG_TAIL, "");

        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.WAG_TAIL, null, false), 2000);

        if (pet.getHappiness() > 40)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_HAPPY));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
