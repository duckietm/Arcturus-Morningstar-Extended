package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WiredTriggerBotReachedHabbo extends InteractionWiredTrigger {
    public final static WiredTriggerType type = WiredTriggerType.BOT_REACHED_AVTR;

    private String botName = "";

    public WiredTriggerBotReachedHabbo(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerBotReachedHabbo(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.botName);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        this.botName = settings.getStringParam();

        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return room.getBots(this.botName).stream().anyMatch(bot -> bot.getRoomUnit() == roomUnit);
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.botName
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.botName = data.botName;
        } else {
            this.botName = wiredData;
        }
    }

    @Override
    public void onPickUp() {
        this.botName = "";
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    static class JsonData {
        String botName;

        public JsonData(String botName) {
            this.botName = botName;
        }
    }
}
