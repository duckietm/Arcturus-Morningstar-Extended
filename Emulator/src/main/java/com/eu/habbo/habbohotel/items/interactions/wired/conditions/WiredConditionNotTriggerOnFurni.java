package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionNotTriggerOnFurni extends WiredConditionTriggerOnFurni {
    public static final WiredConditionType type = WiredConditionType.NOT_ACTOR_ON_FURNI;

    public WiredConditionNotTriggerOnFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotTriggerOnFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null)
            return false;

        this.refresh();

        if (this.items.isEmpty())
            return true;

        return !triggerOnFurni(roomUnit, room);
    }

    @Override
    public WiredConditionType getType() {
        return type;
    }
}
