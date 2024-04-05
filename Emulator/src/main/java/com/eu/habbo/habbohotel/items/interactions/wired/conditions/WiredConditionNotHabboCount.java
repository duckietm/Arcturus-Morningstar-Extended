package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionNotHabboCount extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.NOT_USER_COUNT;

    private int lowerLimit = 10;
    private int upperLimit = 20;

    public WiredConditionNotHabboCount(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotHabboCount(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        int count = room.getUserCount();

        return count < this.lowerLimit || count > this.upperLimit;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.lowerLimit,
                this.upperLimit
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            WiredConditionHabboCount.JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, WiredConditionHabboCount.JsonData.class);
            this.lowerLimit = data.lowerLimit;
            this.upperLimit = data.upperLimit;
        } else {
            String[] data = wiredData.split(":");
            this.lowerLimit = Integer.parseInt(data[0]);
            this.upperLimit = Integer.parseInt(data[1]);
        }
    }

    @Override
    public void onPickUp() {
        this.upperLimit = 0;
        this.lowerLimit = 20;
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
        message.appendInt(2);
        message.appendInt(this.lowerLimit);
        message.appendInt(this.upperLimit);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 2) return false;
        this.lowerLimit = settings.getIntParams()[0];
        this.upperLimit = settings.getIntParams()[1];

        return true;
    }

    static class JsonData {
        int lowerLimit;
        int upperLimit;

        public JsonData(int lowerLimit, int upperLimit) {
            this.lowerLimit = lowerLimit;
            this.upperLimit = upperLimit;
        }
    }
}
