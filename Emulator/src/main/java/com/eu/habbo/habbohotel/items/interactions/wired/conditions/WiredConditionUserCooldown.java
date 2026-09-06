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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passes once per user every N cycles (the dialog slider of the "time elapsed" condition; one
 * cycle = 500 ms). The stack is blocked for that user until the cooldown has expired.
 */
public class WiredConditionUserCooldown extends InteractionWiredCondition {
    private static final WiredConditionType type = WiredConditionType.USER_COOLDOWN;
    private static final int MAX_CYCLES = 1_000_000;

    private int cycles;
    private final Map<Integer, Long> lastPass = new ConcurrentHashMap<>();

    public WiredConditionUserCooldown(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionUserCooldown(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean evaluate(WiredContext ctx) {
        if (ctx == null || ctx.actor().isEmpty() || ctx.room() == null) return true;

        Habbo habbo = ctx.room().getHabbo(ctx.actor().get());
        if (habbo == null) return true;

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(0, this.cycles) * 500L;
        Long last = this.lastPass.get(habbo.getHabboInfo().getId());
        if (last != null && now - last < cooldownMs) return false;

        this.lastPass.put(habbo.getHabboInfo().getId(), now);
        return true;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.cycles));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        this.cycles = 0;
        try {
            if (wiredData != null && wiredData.startsWith("{")) {
                JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
                if (data != null) this.cycles = clamp(data.cycles);
            } else if (wiredData != null && !wiredData.isEmpty()) {
                this.cycles = clamp(Integer.parseInt(wiredData));
            }
        } catch (Exception ignored) {
            this.cycles = 0;
        }
    }

    @Override
    public void onPickUp() {
        this.cycles = 0;
        this.lastPass.clear();
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
        message.appendInt(this.cycles);
        message.appendInt(0);
        message.appendInt(type.code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        if (settings.getIntParams().length < 1) return false;
        this.cycles = clamp(settings.getIntParams()[0]);
        this.lastPass.clear();
        return true;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_CYCLES, value));
    }

    static class JsonData {
        int cycles;

        JsonData(int cycles) {
            this.cycles = cycles;
        }
    }
}
