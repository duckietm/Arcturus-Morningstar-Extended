package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionTorch extends PetAction {
    public ActionTorch() {
        super(null, true);

        this.minimumActionDuration = 1000;
        this.statusToSet.add(RoomUnitStatus.EAT);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        if (pet.getHappyness() < 30) {
            pet.say(pet.getPetData().randomVocal(PetVocalsType.DISOBEY));
            return false;
        }

        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.EAT, null, false), this.minimumActionDuration);
        return true;
    }
}
