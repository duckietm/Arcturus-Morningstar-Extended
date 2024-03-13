package com.eu.habbo.messages.incoming.rooms.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.MonsterplantPet;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.messages.incoming.MessageHandler;

public class ScratchPetEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        final int petId = this.packet.readInt();

        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() == null) {
            return;
        }

        final Pet pet = this.client.getHabbo().getHabboInfo().getCurrentRoom().getPet(petId);

        if (pet == null) {
            return;
        }

        if (this.client.getHabbo().getHabboStats().petRespectPointsToGive > 0 || pet instanceof MonsterplantPet) {
            pet.scratched(this.client.getHabbo());

            // Update the stats to the database.
            Emulator.getThreading().run(pet);
        }
    }
}
