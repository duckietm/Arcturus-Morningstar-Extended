package com.eu.habbo.messages.outgoing.catalog;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class PetBoughtNotificationComposer extends MessageComposer {
    private final Pet pet;
    private final boolean gift;

    public PetBoughtNotificationComposer(Pet pet, boolean gift) {
        this.pet = pet;
        this.gift = gift;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PetBoughtNotificationComposer);
        this.response.appendBoolean(this.gift);
        this.pet.serialize(this.response);
        return this.response;
    }
}
