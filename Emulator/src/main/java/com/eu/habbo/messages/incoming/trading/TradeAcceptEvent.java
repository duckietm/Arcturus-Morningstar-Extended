package com.eu.habbo.messages.incoming.trading;

import com.eu.habbo.habbohotel.rooms.RoomTrade;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;

public class TradeAcceptEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();

        if (habbo == null || habbo.getHabboInfo() == null || habbo.getHabboInfo().getCurrentRoom() == null)
            return;

        RoomTrade trade = habbo.getHabboInfo().getCurrentRoom().getActiveTradeForHabbo(habbo);

        if (trade == null)
            return;

        trade.accept(habbo, true);
    }
}
