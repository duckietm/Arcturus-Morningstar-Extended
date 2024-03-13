package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.ClothItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.procedure.TIntProcedure;

import java.util.ArrayList;

public class UserClothesComposer extends MessageComposer {
    private final ArrayList<Integer> idList = new ArrayList<>();
    private final ArrayList<String> nameList = new ArrayList<>();

    public UserClothesComposer(Habbo habbo) {
        habbo.getInventory().getWardrobeComponent().getClothing().forEach(new TIntProcedure() {
            @Override
            public boolean execute(int value) {
                ClothItem item = Emulator.getGameEnvironment().getCatalogManager().clothing.get(value);

                if (item != null) {
                    for (Integer j : item.setId) {
                        UserClothesComposer.this.idList.add(j);
                    }

                    UserClothesComposer.this.nameList.add(item.name);
                }

                return true;
            }
        });
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserClothesComposer);
        this.response.appendInt(this.idList.size());
        this.idList.forEach(this.response::appendInt);
        this.response.appendInt(this.nameList.size());
        this.nameList.forEach(this.response::appendString);
        return this.response;
    }
}
