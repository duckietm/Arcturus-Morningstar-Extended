package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSuper;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionSuper extends InteractionWiredCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(WiredConditionSuper.class);
    private static final WiredConditionType WIRED_EFFECT_TYPE = WiredConditionType.ACTOR_WEARS_BADGE;
    private static final int CONFIG_MAX_LENGTH = 100;

    private String configKey = "";
    private String configValue = "";

    public WiredConditionSuper(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionSuper(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public WiredConditionType getType() {
        return WIRED_EFFECT_TYPE;
    }

    @Override
    public boolean saveData(WiredSettings settings)  {
        final String configuration = settings.getStringParam();

        if (configuration.length() > CONFIG_MAX_LENGTH) {
            return false;
        }

        if (configuration.contains(":")) {
            final String[] parts = configuration.split(":", 2);

            this.configKey = parts[0];
            this.configValue = parts[1];
        } else {
            this.configKey = configuration;
            this.configValue = "";
        }

        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        LOGGER.debug("Executing super wired condition, key {} value {}", this.configKey, this.configValue);

        try {
            switch (this.configKey) {
                case "nototalclassement": {
                    return WiredSuper.noTotalClassement(roomUnit, room);
                }

                case "totalpointequal": {
                    final int score = Integer.parseInt(configValue);
                    return WiredSuper.totalPointEqual(roomUnit, room, score);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to run super wired condition " + this.configKey, e);
        }

        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new WiredConditionSuper.JsonData(this.configKey, this.configValue));
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.getConfiguration());
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        final String wiredData = set.getString("wired_data");
        final WiredConditionSuper.JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, WiredConditionSuper.JsonData.class);

        this.configKey = data.configKey;
        this.configValue = data.configValue;
    }

    @Override
    public void onPickUp() {
        this.configKey = "";
        this.configValue = "";
    }

    private String getConfiguration() {
        if ("".equals(this.configKey)) {
            return this.configKey;
        }

        return this.configKey + ":" + this.configValue;
    }

    static class JsonData {
        String configKey;
        String configValue;

        public JsonData(String configKey, String configValue) {
            this.configKey = configKey;
            this.configValue = configValue;
        }
    }
}
