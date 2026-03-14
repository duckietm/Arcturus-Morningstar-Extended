package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionSpin extends PetAction {
    public ActionSpin() {
        super(PetTasks.FREE, true);
        this.minimumActionDuration = 2000;
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();

        // Spin animation - rotate through all directions
        for (int i = 0; i < 8; i++) {
            final int rotation = i;
            Emulator.getThreading().run(() -> {
                pet.getRoomUnit().setRotation(RoomUserRotation.values()[rotation]);
                pet.packetUpdate = true;
            }, i * 250);
        }

        if (pet.getHappiness() > 50)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
