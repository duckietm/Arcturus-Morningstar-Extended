package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.outgoing.users.InClientLinkComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Opens Nitro's localized hand-item browser from the handitem_tester furni. */
public class InteractionHanditemTester extends InteractionDefault {
    public InteractionHanditemTester(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public InteractionHanditemTester(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        super.onClick(client, room, objects);
        if (client != null) client.sendResponse(new InClientLinkComposer("avatar-handitems/show"));
    }
}
