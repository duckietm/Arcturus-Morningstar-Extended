package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** QuestCompleted (949): the finished quest and whether the client shows the completed dialog. */
public class QuestCompletedComposer extends MessageComposer {
    private final QuestsComposer.Quest quest;
    private final boolean showDialog;

    public QuestCompletedComposer(QuestsComposer.Quest quest, boolean showDialog) {
        this.quest = quest;
        this.showDialog = showDialog;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestCompletedComposer);
        this.response.append(this.quest);
        this.response.appendBoolean(this.showDialog);
        return this.response;
    }

    public QuestsComposer.Quest getQuest() {
        return quest;
    }

    public boolean isShowDialog() {
        return showDialog;
    }
}
