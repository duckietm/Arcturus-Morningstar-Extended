package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionBreatheFire extends PetAction {
    public ActionBreatheFire() {
        super(null, true);
        this.minimumActionDuration = 1000;
        this.statusToSet.add(RoomUnitStatus.FLAME);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.FLAME, null, false), this.minimumActionDuration);

        if (pet.getHappyness() > 50)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));

        return true;
    }
}
