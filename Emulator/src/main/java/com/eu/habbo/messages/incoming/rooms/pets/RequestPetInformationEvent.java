package com.eu.habbo.messages.incoming.rooms.pets;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetData;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.pets.PetInformationComposer;

public class RequestPetInformationEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int petId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null)
            return;

        Pet pet = room.getPet(petId);

        if (pet != null) {
            this.client.sendResponse(new PetInformationComposer(pet, room, this.client.getHabbo()));
            return;
        }

        // Users morphed with :transform are announced as pets whose id is the user id;
        // answer with a transient pet so the pet infostand opens for them too.
        Habbo morphed = room.getHabbo(petId);
        if (morphed == null || morphed.getRoomUnit() == null || morphed.getRoomUnit().getRoomUnitType() != RoomUnitType.PET) return;

        Object type = morphed.getHabboStats().cache.get("pet_type");
        Object race = morphed.getHabboStats().cache.get("pet_race");
        Object color = morphed.getHabboStats().cache.get("pet_color");
        if (!(type instanceof PetData)) return;

        Pet ghost = new Pet(
                ((PetData) type).getType(),
                race instanceof Integer ? (Integer) race : 0,
                color instanceof String ? (String) color : "FFFFFF",
                morphed.getHabboInfo().getUsername(),
                morphed.getHabboInfo().getId());
        ghost.setId(morphed.getHabboInfo().getId());
        this.client.sendResponse(new PetInformationComposer(ghost, room, this.client.getHabbo()));
    }
}
