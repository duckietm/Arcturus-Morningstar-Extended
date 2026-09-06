package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passes once per user per calendar day (hotel local time). The last pass day is persisted in
 * wired_data so a room reload does not hand out a second daily run.
 */
public class WiredConditionDailyTrigger extends InteractionWiredCondition {
    private static final WiredConditionType type = WiredConditionType.DAILY_TRIGGER;

    private final Map<Integer, Long> lastDayByUser = new ConcurrentHashMap<>();

    public WiredConditionDailyTrigger(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionDailyTrigger(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean evaluate(WiredContext ctx) {
        if (ctx == null || ctx.actor().isEmpty() || ctx.room() == null) return false;

        Habbo habbo = ctx.room().getHabbo(ctx.actor().get());
        if (habbo == null) return false;

        long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        Long last = this.lastDayByUser.get(habbo.getHabboInfo().getId());
        if (last != null && last == today) return false;

        this.lastDayByUser.put(habbo.getHabboInfo().getId(), today);
        this.needsUpdate(true);
        return true;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.lastDayByUser));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.lastDayByUser.clear();
        String wiredData = set.getString("wired_data");
        if (wiredData == null || !wiredData.startsWith("{")) return;
        try {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            if (data != null && data.lastDayByUser != null) this.lastDayByUser.putAll(data.lastDayByUser);
        } catch (Exception ignored) {
            this.lastDayByUser.clear();
        }
    }

    @Override
    public void onPickUp() {
        this.lastDayByUser.clear();
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
        message.appendInt(type.code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        return true;
    }

    static class JsonData {
        Map<Integer, Long> lastDayByUser;

        JsonData(Map<Integer, Long> lastDayByUser) {
            this.lastDayByUser = lastDayByUser;
        }
    }
}
