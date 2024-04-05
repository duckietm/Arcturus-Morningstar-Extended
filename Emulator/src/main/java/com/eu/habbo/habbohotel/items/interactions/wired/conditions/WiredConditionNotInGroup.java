package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionNotInGroup extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.NOT_ACTOR_IN_GROUP;

    public WiredConditionNotInGroup(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotInGroup(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room.getGuildId() == 0)
            return false;

        Habbo habbo = room.getHabbo(roomUnit);

        return habbo == null || !habbo.getHabboStats().hasGuild(room.getGuildId());
    }

    @Override
    public String getWiredData() {
        return "";
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {

    }

    @Override
    public void onPickUp() {

    }

    @Override
    public WiredConditionType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        return true;
    }
}
