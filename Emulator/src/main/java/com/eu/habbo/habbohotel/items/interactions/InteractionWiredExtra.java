package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredAddonType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.outgoing.wired.WiredAddonDataComposer;
import com.eu.habbo.messages.outgoing.wired.WiredConditionDataComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class InteractionWiredExtra extends InteractionWired {
    public InteractionWiredExtra(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionWiredExtra(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        if (client != null) {
            if (room.hasRights(client.getHabbo())) {
                client.sendResponse(new WiredAddonDataComposer(this, room));
                this.activateBox(room);
            }
        }
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    public abstract boolean saveData(ClientMessage packet);
    public abstract WiredAddonType getType();

    @Override
    public boolean isWalkable() {
        return true;
    }
}