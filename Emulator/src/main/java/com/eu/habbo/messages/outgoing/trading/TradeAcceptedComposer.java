package com.eu.habbo.messages.outgoing.trading;

import com.eu.habbo.habbohotel.rooms.RoomTradeUser;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class TradeAcceptedComposer extends MessageComposer {
    private final RoomTradeUser tradeUser;

    public TradeAcceptedComposer(RoomTradeUser tradeUser) {
        this.tradeUser = tradeUser;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.TradeAcceptedComposer);
        this.response.appendInt(this.tradeUser.getUserId());
        this.response.appendInt(this.tradeUser.getAccepted());
        return this.response;
    }
}
