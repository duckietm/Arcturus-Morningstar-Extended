package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionTripleJump extends PetAction {
    public ActionTripleJump() {
        super(PetTasks.JUMP, true);
        this.minimumActionDuration = 4000;
        this.statusToSet.add(RoomUnitStatus.JUMP);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();

        // Triple jump - three jump animations
        for (int i = 0; i < 3; i++) {
            Emulator.getThreading().run(() -> {
                pet.getRoomUnit().setStatus(RoomUnitStatus.JUMP, "");
                pet.packetUpdate = true;
            }, i * 1200);
        }

        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.JUMP, null, false), 4000);

        if (pet.getHappiness() > 70)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
