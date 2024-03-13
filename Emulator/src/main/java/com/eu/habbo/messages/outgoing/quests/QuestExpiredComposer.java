package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class QuestExpiredComposer extends MessageComposer {
    private final boolean expired;

    public QuestExpiredComposer(boolean expired) {
        this.expired = expired;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestExpiredComposer);
        this.response.appendBoolean(this.expired);
        return this.response;
    }
}