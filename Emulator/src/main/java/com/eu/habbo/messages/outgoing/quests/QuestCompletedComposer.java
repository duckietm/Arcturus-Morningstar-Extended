package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class QuestCompletedComposer extends MessageComposer {
    private final UnknownClass unknownClass;
    private final boolean unknowbOolean;

    public QuestCompletedComposer(UnknownClass unknownClass, boolean unknowbOolean) {
        this.unknownClass = unknownClass;
        this.unknowbOolean = unknowbOolean;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestCompletedComposer);

        return this.response;
    }

    public static class UnknownClass implements ISerialize {
        private final int activityPointsType;
        private final boolean accepted;
        private final int id;
        private final String type;
        private final int sortOrder;
        private final boolean easy;

        public UnknownClass(int activityPointsType, boolean accepted, int id, String type, int sortOrder, boolean easy) {
            this.activityPointsType = activityPointsType;
            this.accepted = accepted;
            this.id = id;
            this.type = type;
            this.sortOrder = sortOrder;
            this.easy = easy;
        }

        @Override
        public void serialize(ServerMessage message) {
            message.appendString("");
            message.appendInt(0);
            message.appendInt(0);
            message.appendInt(this.activityPointsType);
            message.appendInt(this.id);
            message.appendBoolean(this.accepted);
            message.appendString(this.type);
            message.appendString("");
            message.appendInt(0);
            message.appendString("");
            message.appendInt(0);
            message.appendInt(0);
            message.appendInt(this.sortOrder);
            message.appendString("");
            message.appendString("");
            message.appendBoolean(this.easy);
        }

    }
}