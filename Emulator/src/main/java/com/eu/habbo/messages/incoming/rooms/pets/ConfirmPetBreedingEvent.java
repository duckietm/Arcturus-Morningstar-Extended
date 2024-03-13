package com.eu.habbo.messages.incoming.rooms.pets;

import com.eu.habbo.habbohotel.items.interactions.pets.InteractionPetBreedingNest;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;

public class ConfirmPetBreedingEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();
        String name = this.packet.readString();
        int petOneId = this.packet.readInt();
        int petTwoId = this.packet.readInt();

        HabboItem item = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabboItem(itemId);

        if (item instanceof InteractionPetBreedingNest) {
            ((InteractionPetBreedingNest) item).breed(this.client.getHabbo(), name, petOneId, petTwoId);
        }
    }
}