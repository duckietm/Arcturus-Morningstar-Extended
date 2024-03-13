package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.habbohotel.pets.PetRace;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.set.hash.THashSet;

public class PetBreedsComposer extends MessageComposer {
    private final String petName;
    private final THashSet<PetRace> petRaces;

    public PetBreedsComposer(String petName, THashSet<PetRace> petRaces) {
        this.petName = petName;
        this.petRaces = petRaces;
    }

    @Override
    protected ServerMessage composeInternal() {
        if (this.petRaces == null)
            return null;
        this.response.init(Outgoing.PetBreedsComposer);
        this.response.appendString(this.petName);
        this.response.appendInt(this.petRaces.size());
        for (PetRace race : this.petRaces) {
            this.response.appendInt(race.race);
            this.response.appendInt(race.colorOne);
            this.response.appendInt(race.colorTwo);
            this.response.appendBoolean(race.hasColorOne);
            this.response.appendBoolean(race.hasColorTwo);
        }
        return this.response;
    }
}
