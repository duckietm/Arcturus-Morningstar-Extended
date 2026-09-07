package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** QuestCancelled (3027): the tracked quest stops; `expired` makes the client show the expiry alert. */
public class QuestExpiredComposer extends MessageComposer {
    private final boolean expired;
    private final QuestsComposer.Quest quest;

    public QuestExpiredComposer(boolean expired, QuestsComposer.Quest quest) {
        this.expired = expired;
        this.quest = quest;
    }

    /** @deprecated plugin ABI; sends the flag alone, as before the quest block was added. */
    @Deprecated
    public QuestExpiredComposer(boolean expired) {
        this(expired, null);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestExpiredComposer);
        this.response.appendBoolean(this.expired);
        if (this.quest != null) this.response.append(this.quest);
        return this.response;
    }

    public boolean isExpired() {
        return expired;
    }

    public QuestsComposer.Quest getQuest() {
        return quest;
    }
}
