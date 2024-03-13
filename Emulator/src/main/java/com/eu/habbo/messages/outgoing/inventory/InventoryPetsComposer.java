package com.eu.habbo.messages.outgoing.inventory;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.iterator.TIntObjectIterator;

import java.util.NoSuchElementException;

public class InventoryPetsComposer extends MessageComposer {
    private final Habbo habbo;

    public InventoryPetsComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.InventoryPetsComposer);

        this.response.appendInt(1);
        this.response.appendInt(1);
        this.response.appendInt(this.habbo.getInventory().getPetsComponent().getPetsCount());

        TIntObjectIterator<Pet> petIterator = this.habbo.getInventory().getPetsComponent().getPets().iterator();

        for (int i = this.habbo.getInventory().getPetsComponent().getPets().size(); i-- > 0; ) {
            try {
                petIterator.advance();
            } catch (NoSuchElementException e) {
                break;
            }
            petIterator.value().serialize(this.response);
        }

        return this.response;
    }
}
