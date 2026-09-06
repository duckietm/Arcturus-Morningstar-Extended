package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.messages.ServerMessage;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Passes only the first time the stack runs after the room timers were reset (wf_act_reset_timers
 * or a room reload). No parameters: reuses the parameter-less NO_BATTLEBANZAI dialog.
 */
public class WiredConditionFirstTrigger extends InteractionWiredCondition {
    private static final WiredConditionType type = WiredConditionType.FIRST_TRIGGER;

    private int passedAt = 0;

    public WiredConditionFirstTrigger(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionFirstTrigger(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean evaluate(WiredContext ctx) {
        if (ctx == null || ctx.room() == null) return false;

        if (this.passedAt > 0 && this.passedAt >= ctx.room().getLastTimerReset()) return false;

        this.passedAt = Emulator.getIntUnixTimestamp();
        return true;
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return "";
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.passedAt = 0;
    }

    @Override
    public void onPickUp() {
        this.passedAt = 0;
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
        this.passedAt = 0;
        return true;
    }
}
