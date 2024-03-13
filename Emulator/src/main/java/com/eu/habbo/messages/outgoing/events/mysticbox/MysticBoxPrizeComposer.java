package com.eu.habbo.messages.outgoing.events.mysticbox;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class MysticBoxPrizeComposer extends MessageComposer {
    private final String type;
    private final int itemId;

    public MysticBoxPrizeComposer(String type, int itemId) {
        this.type = type;
        this.itemId = itemId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.MysticBoxPrizeComposer);
        this.response.appendString(this.type);
        this.response.appendInt(this.itemId);
        return this.response;
    }
}
