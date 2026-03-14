package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetTree;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.threading.runnables.PetClearPosture;

public class ActionHang extends PetAction {
    public ActionHang() {
        super(PetTasks.FREE, true);
        this.minimumActionDuration = 4000;
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        // Hang requires pet to be on a pet tree (dragon/monkey tree)
        if (pet.getRoom() == null) {
            return false;
        }
        
        HabboItem itemBelow = pet.getRoom().getTopItemAt(pet.getRoomUnit().getX(), pet.getRoomUnit().getY());
        if (!(itemBelow instanceof InteractionPetTree)) {
            // Pet must go to tree first
            pet.findTree();
            pet.say(pet.getPetData().randomVocal(PetVocalsType.DISOBEY));
            return false;
        }
        
        pet.clearPosture();
        pet.getRoomUnit().setStatus(RoomUnitStatus.HANG, "");

        Emulator.getThreading().run(new PetClearPosture(pet, RoomUnitStatus.HANG, null, false), 4000);

        if (pet.getHappiness() > 50)
            pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
        else
            pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

        return true;
    }
}
