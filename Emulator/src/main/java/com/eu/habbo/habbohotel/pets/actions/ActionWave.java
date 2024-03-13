package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionWave extends PetAction {
    public ActionWave() {
        super(PetTasks.WAVE, false);

        this.statusToSet.add(RoomUnitStatus.WAVE);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        //WAV
        if (pet.getHappyness() > 65) {
            pet.getRoomUnit().setStatus(RoomUnitStatus.WAVE, "0");

            Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.WAVE, null, false), 2000);
            return true;
        }

        return false;
    }
}
