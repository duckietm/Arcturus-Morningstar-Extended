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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionHabboHasHandItem extends InteractionWiredCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredConditionHabboHasHandItem.class);

    public static final WiredConditionType type = WiredConditionType.ACTOR_HAS_HANDITEM;

    private int handItem;

    public WiredConditionHabboHasHandItem(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionHabboHasHandItem(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
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
        message.appendInt(1);
        message.appendInt(this.handItem);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if(settings.getIntParams().length < 1) return false;
        this.handItem = settings.getIntParams()[0];

        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null) return false;
        return roomUnit.getHandItem() == this.handItem;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.handItem
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        try {
            String wiredData = set.getString("wired_data");

            if (wiredData.startsWith("{")) {
                JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
                this.handItem = data.handItemId;
            } else {
                this.handItem = Integer.parseInt(wiredData);
            }
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    @Override
    public void onPickUp() {
        this.handItem = 0;
    }

    static class JsonData {
        int handItemId;

        public JsonData(int handItemId) {
            this.handItemId = handItemId;
        }
    }
}
