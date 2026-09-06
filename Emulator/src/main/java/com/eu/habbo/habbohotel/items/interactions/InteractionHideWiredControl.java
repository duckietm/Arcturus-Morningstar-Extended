package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionHideWiredControl extends InteractionRemoteSwitchControl {
    public InteractionHideWiredControl(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionHideWiredControl(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    /** Switching the controller hides or shows wired, tile collision included. */
    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        super.onClick(client, room, objects);

        if (room != null) {
            room.refreshWiredHidden();
        }
    }

    @Override
    public void onPickUp(Room room) {
        super.onPickUp(room);

        if (room != null) {
            room.refreshWiredHidden();
        }
    }
}
