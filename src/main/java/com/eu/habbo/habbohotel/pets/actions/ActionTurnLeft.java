package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionTurnLeft extends PetAction {
    public ActionTurnLeft() {
        super(null, true);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        pet.getRoomUnit().setBodyRotation(RoomUserRotation.values()[(pet.getRoomUnit().getBodyRotation().getValue() - 1 < 0 ? 7 : pet.getRoomUnit().getBodyRotation().getValue() - 1)]);
        pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));
        return true;
    }
}
