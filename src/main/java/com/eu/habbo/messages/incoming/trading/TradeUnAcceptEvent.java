package com.eu.habbo.messages.incoming.trading;

import com.eu.habbo.habbohotel.rooms.RoomTrade;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;

public class TradeUnAcceptEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        RoomTrade trade = habbo.getHabboInfo().getCurrentRoom().getActiveTradeForHabbo(habbo);

        if (trade == null)
            return;

        trade.accept(habbo, false);
    }
}
