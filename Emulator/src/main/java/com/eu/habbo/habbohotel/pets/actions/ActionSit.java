package com.eu.habbo.habbohotel.pets.actions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetAction;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;

public class ActionSit extends PetAction {
    public ActionSit() {
        super(PetTasks.SIT, true);
        this.minimumActionDuration = 4000;
        this.statusToRemove.add(RoomUnitStatus.BEG);
        this.statusToRemove.add(RoomUnitStatus.MOVE);
        this.statusToRemove.add(RoomUnitStatus.LAY);
        this.statusToRemove.add(RoomUnitStatus.DEAD);
    }

    @Override
    public boolean apply(Pet pet, Habbo habbo, String[] data) {
        if (pet.getTask() != PetTasks.SIT && !pet.getRoomUnit().hasStatus(RoomUnitStatus.SIT)) {
            pet.getRoomUnit().cmdSit = true;
            pet.getRoomUnit().setStatus(RoomUnitStatus.SIT, pet.getRoomUnit().getCurrentLocation().getStackHeight() + "");

            // Sitting is a bit boring
            pet.addHappiness(-2);

            Emulator.getThreading().run(() -> {
                pet.getRoomUnit().cmdSit = false;
                pet.clearPosture();
            }, this.minimumActionDuration);

            if (pet.getHappiness() > 75)
                pet.say(pet.getPetData().randomVocal(PetVocalsType.PLAYFUL));
            else
                pet.say(pet.getPetData().randomVocal(PetVocalsType.GENERIC_NEUTRAL));

            return true;
        }

        return true;
    }
}
