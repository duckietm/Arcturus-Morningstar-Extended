package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.outgoing.wired.WiredTriggerDataComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class InteractionWiredTrigger extends InteractionWired {
    private int delay;

    protected InteractionWiredTrigger(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    protected InteractionWiredTrigger(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        if (client != null) {
            if (room.hasRights(client.getHabbo())) {
                client.sendResponse(new WiredTriggerDataComposer(this, room));
                this.activateBox(room);
            }
        }
    }

    public abstract WiredTriggerType getType();

    public abstract boolean saveData(WiredSettings settings);

    protected int getDelay() {
        return this.delay;
    }

    protected void setDelay(int value) {
        this.delay = value;
    }

    public boolean isTriggeredByRoomUnit() {
        return false;
    }

}
