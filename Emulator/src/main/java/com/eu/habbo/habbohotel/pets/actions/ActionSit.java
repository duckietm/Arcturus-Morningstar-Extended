package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionSit extends PetAction {
    public ActionSit() {
        super(PetTasks.SIT, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        if (pet.getTask() != PetTasks.SIT && !pet.getRoomUnit().hasStatus(RoomUnitStatus.SIT)) {
            pet.getRoomUnit().setStatus(RoomUnitStatus.SIT, pet.getRoom().getStackHeight(pet.getRoomUnit().getX(), pet.getRoomUnit().getY(), false) - 0.50 + "");

            if (pet.getHappyness() > 75)
                pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
            else
                pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

            return true;
        }

        return false;
    }
}
