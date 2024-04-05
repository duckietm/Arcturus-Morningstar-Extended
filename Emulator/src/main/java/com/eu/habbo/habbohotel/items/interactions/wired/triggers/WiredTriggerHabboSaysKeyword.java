package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredTriggerHabboSaysKeyword extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.SAY_SOMETHING;

    private boolean ownerOnly = false;
    private String key = "";

    public WiredTriggerHabboSaysKeyword(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerHabboSaysKeyword(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (this.key.length() > 0) {
            if (stuff[0] instanceof String) {
                if (((String) stuff[0]).toLowerCase().contains(this.key.toLowerCase())) {
                    Habbo habbo = room.getHabbo(roomUnit);
                    return !this.ownerOnly || (habbo != null && room.getOwnerId() == habbo.getHabboInfo().getId());
                }
            }
        }
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.ownerOnly,
            this.key
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.ownerOnly = data.ownerOnly;
            this.key = data.key;
        } else {
            String[] data = wiredData.split("\t");

            if (data.length == 2) {
                this.ownerOnly = data[0].equalsIgnoreCase("1");
                this.key = data[1];
            }
        }
    }

    @Override
    public void onPickUp() {
        this.ownerOnly = false;
        this.key = "";
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
        message.appendString(this.key);
        message.appendInt(0);
        message.appendInt(1);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 1) return false;
        this.ownerOnly = settings.getIntParams()[0] == 1;
        this.key = settings.getStringParam();

        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    static class JsonData {
        boolean ownerOnly;
        String key;

        public JsonData(boolean ownerOnly, String key) {
            this.ownerOnly = ownerOnly;
            this.key = key;
        }
    }
}
