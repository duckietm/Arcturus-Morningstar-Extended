package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class QuestComposer extends MessageComposer {
    private final QuestsComposer.Quest quest;

    public QuestComposer(QuestsComposer.Quest quest) {
        this.quest = quest;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestComposer);
        this.response.append(this.quest);
        return this.response;
    }
}