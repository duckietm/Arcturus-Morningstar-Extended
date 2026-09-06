package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredNumericInputGuard;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Gives the resolved users the hotel's Punti currency (type 103). */
public class WiredEffectGiveBssPoints extends InteractionWiredEffect {
    public static final int POINTS_TYPE = 103;
    public static final WiredEffectType type = WiredEffectType.SHOW_MESSAGE;

    private int amount;
    private int userSource = WiredSourceUtil.SOURCE_TRIGGER;

    public WiredEffectGiveBssPoints(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveBssPoints(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(Integer.toString(this.amount));
        message.appendInt(1);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(type.code);
        message.appendInt(this.getDelay());

        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            for (InteractionWiredTrigger object : room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY())) {
                if (!object.isTriggeredByRoomUnit()) invalidTriggers.add(object.getBaseItem().getSpriteId());
            }
            message.appendInt(invalidTriggers.size());
            for (Integer trigger : invalidTriggers) message.appendInt(trigger);
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) {
        int nextAmount = WiredNumericInputGuard.parsePositiveAmount(
                settings.getStringParam(), WiredNumericInputGuard.maxRewardAmount());
        if (nextAmount <= 0) return false;

        this.amount = nextAmount;
        int[] params = settings.getIntParams();
        this.userSource = params.length > 0 ? params[0] : WiredSourceUtil.SOURCE_TRIGGER;
        this.setDelay(settings.getDelay());
        return true;
    }

    @Override
    public void execute(WiredContext context) {
        Room room = context.room();
        if (room == null || this.amount <= 0) return;

        for (RoomUnit unit : WiredSourceUtil.resolveUsers(context, this.userSource)) {
            Habbo habbo = room.getHabbo(unit);
            if (habbo != null) habbo.givePoints(POINTS_TYPE, this.amount);
        }
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(this.amount, this.getDelay(), this.userSource));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.amount = Math.max(0, Math.min(data.amount, WiredNumericInputGuard.maxRewardAmount()));
            this.setDelay(data.delay);
            this.userSource = data.userSource;
        } else {
            this.onPickUp();
        }
    }

    @Override
    public void onPickUp() {
        this.amount = 0;
        this.userSource = WiredSourceUtil.SOURCE_TRIGGER;
        this.setDelay(0);
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredSourceUtil.SOURCE_TRIGGER;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    static class JsonData {
        int amount;
        int delay;
        int userSource;

        JsonData(int amount, int delay, int userSource) {
            this.amount = amount;
            this.delay = delay;
            this.userSource = userSource;
        }
    }
}
