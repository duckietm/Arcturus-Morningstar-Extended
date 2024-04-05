package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionDateRangeActive extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.DATE_RANGE;

    private int startDate;
    private int endDate;

    public WiredConditionDateRangeActive(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionDateRangeActive(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
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
        message.appendInt(this.startDate);
        message.appendInt(this.endDate);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.startDate);
        message.appendInt(this.endDate);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 2) return false;
        this.startDate = settings.getIntParams()[0];
        this.endDate = settings.getIntParams()[1];
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        int time = Emulator.getIntUnixTimestamp();
        return this.startDate < time && this.endDate >= time;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.startDate,
                this.endDate
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.startDate = data.startDate;
            this.endDate = data.endDate;
        } else {
            String[] data = wiredData.split("\t");

            if (data.length == 2) {
                try {
                    this.startDate = Integer.parseInt(data[0]);
                    this.endDate = Integer.parseInt(data[1]);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.startDate = 0;
        this.endDate = 0;
    }

    static class JsonData {
        int startDate;
        int endDate;

        public JsonData(int startDate, int endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}
