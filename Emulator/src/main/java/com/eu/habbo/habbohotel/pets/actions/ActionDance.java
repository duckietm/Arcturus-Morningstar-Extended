package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionDance extends PetAction {
    public ActionDance() {
        super(PetTasks.FREE, true);
        this.minimumActionDuration = 5000;
        this.statusToSet.add(RoomUnitStatus.DANCE);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();
        pet.getRoomUnit().setStatus(RoomUnitStatus.DANCE, "");

        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.DANCE, null, false), 5000);

        // Dancing is fun!
        pet.addHappiness(10);

        if (pet.getHappiness() > 60)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
