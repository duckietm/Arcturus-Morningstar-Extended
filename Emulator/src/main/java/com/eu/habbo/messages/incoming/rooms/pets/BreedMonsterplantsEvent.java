package com.eu.habbo.messages.incoming.rooms.pets;

import com.eu.habbo.habbohotel.pets.MonsterplantPet;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.messages.incoming.MessageHandler;

public class BreedMonsterplantsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int unknownInt = this.packet.readInt(); //Something state. 2 = accept

        if (unknownInt == 0) {
            Pet petOne = this.client.getHabbo().getHabboInfo().getCurrentRoom().getPet(this.packet.readInt());
            Pet petTwo = this.client.getHabbo().getHabboInfo().getCurrentRoom().getPet(this.packet.readInt());

            if (petOne == null || petTwo == null || petOne == petTwo) {
                //TODO Add error
                return;
            }

            if (petOne instanceof MonsterplantPet && petTwo instanceof MonsterplantPet) {
                ((MonsterplantPet) petOne).breed((MonsterplantPet) petTwo);
            }
        }
    }
}
