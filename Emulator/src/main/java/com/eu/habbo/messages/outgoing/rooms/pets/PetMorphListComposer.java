package com.eu.habbo.messages.outgoing.rooms.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.pets.PetData;
import com.eu.habbo.habbohotel.pets.PetManager;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Header 10090 (custom): the pet types a user can morph into with {@code :transform},
 * plus the user's current morph. Layout: int count; per pet: int type, string name,
 * int breedCount, bool available; then int currentType (-1 when not morphed),
 * int currentRace, string currentColor.
 */
public class PetMorphListComposer extends MessageComposer {
    private final Habbo habbo;

    public PetMorphListComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        PetManager petManager = Emulator.getGameEnvironment().getPetManager();
        List<PetData> pets = new ArrayList<>(petManager.getPetData());
        Collections.sort(pets);

        this.response.init(Outgoing.PetMorphListComposer);
        this.response.appendInt(pets.size());
        for (PetData pet : pets) {
            int races = petManager.getRaceCount(pet.getType());
            this.response.appendInt(pet.getType());
            this.response.appendString(pet.getName());
            this.response.appendInt(Math.max(1, races));
            this.response.appendBoolean(races > 0);
        }

        int currentType = -1;
        int currentRace = 0;
        String currentColor = "FFFFFF";
        if (this.habbo.getRoomUnit() != null && this.habbo.getRoomUnit().getRoomUnitType() == RoomUnitType.PET) {
            Object type = this.habbo.getHabboStats().cache.get("pet_type");
            Object race = this.habbo.getHabboStats().cache.get("pet_race");
            Object color = this.habbo.getHabboStats().cache.get("pet_color");
            if (type instanceof PetData) currentType = ((PetData) type).getType();
            if (race instanceof Integer) currentRace = (Integer) race;
            if (color instanceof String) currentColor = (String) color;
        }
        this.response.appendInt(currentType);
        this.response.appendInt(currentRace);
        this.response.appendString(currentColor);
        return this.response;
    }
}
