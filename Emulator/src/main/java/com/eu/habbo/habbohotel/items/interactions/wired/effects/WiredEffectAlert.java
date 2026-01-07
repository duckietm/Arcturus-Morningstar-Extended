package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectAlert extends WiredEffectWhisper {
    public WiredEffectAlert(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectAlert(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        Habbo habbo = ctx.actor().map(room::getHabbo).orElse(null);

        if (habbo != null) {
            habbo.alert(this.message
                    .replace("%online%", Emulator.getGameEnvironment().getHabboManager().getOnlineCount() + "")
                    .replace("%username%", habbo.getHabboInfo().getUsername())
                    .replace("%roomsloaded%", Emulator.getGameEnvironment().getRoomManager().loadedRoomsCount() + ""));
        }
    }
}
