package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionStay extends PetAction {
    public ActionStay() {
        super(PetTasks.STAY, true);

        this.statusToRemove.remove(RoomUnitStatus.MOVE);
        this.statusToRemove.remove(RoomUnitStatus.DEAD);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.clearPosture();

        pet.getRoomUnit().setCanWalk(false);
        pet.setStayStartedAt(Emulator.getIntUnixTimestamp());
        pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
