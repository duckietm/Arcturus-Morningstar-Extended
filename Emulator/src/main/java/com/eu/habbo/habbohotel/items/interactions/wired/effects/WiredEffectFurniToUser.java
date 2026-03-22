package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.FurnitureMovementError;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredMoveCarryHelper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectFurniToUser extends WiredEffectUserFurniBase {
    public static final WiredEffectType type = WiredEffectType.FURNI_TO_USER;

    public WiredEffectFurniToUser(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectFurniToUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        HabboItem item = this.resolveLastItem(ctx);
        Habbo habbo = this.resolveLastHabbo(room, ctx);

        if (room == null || item == null || habbo == null || habbo.getRoomUnit() == null) {
            return;
        }

        RoomTile targetTile = habbo.getRoomUnit().getCurrentLocation();
        if (targetTile == null) {
            return;
        }

        FurnitureMovementError error = WiredMoveCarryHelper.moveFurni(room, this, item, targetTile, item.getRotation(), null, false, ctx);
        if (error == FurnitureMovementError.NONE) {
            return;
        }

        if (item.getBaseItem().getStateCount() > 0) {
            error = WiredMoveCarryHelper.moveFurni(room, this, item, targetTile, item.getRotation(), item.getZ(), null, false, ctx);
            if (error == FurnitureMovementError.NONE) {
                return;
            }
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
