package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AddPetComposer extends MessageComposer {
    private final Pet pet;

    public AddPetComposer(Pet pet) {
        this.pet = pet;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AddPetComposer);
        this.pet.serialize(this.response);
        this.response.appendBoolean(false);
        return this.response;
    }
}
