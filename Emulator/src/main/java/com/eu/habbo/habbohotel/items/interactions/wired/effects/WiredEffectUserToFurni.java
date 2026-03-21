package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectUserToFurni extends WiredEffectUserFurniBase {
    public static final WiredEffectType type = WiredEffectType.USER_TO_FURNI;

    public WiredEffectUserToFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectUserToFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        HabboItem item = this.resolveLastItem(ctx);

        if (room == null || item == null) {
            return;
        }

        for (Habbo habbo : this.resolveHabbos(room, ctx)) {
            room.teleportHabboToItem(habbo, item);
        }
    }

    @Deprecated
    @Override
    public boolean execute(com.eu.habbo.habbohotel.rooms.RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }
}
