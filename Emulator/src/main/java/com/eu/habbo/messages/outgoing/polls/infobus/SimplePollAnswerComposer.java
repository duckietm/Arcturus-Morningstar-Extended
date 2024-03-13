package com.eu.habbo.messages.outgoing.polls.infobus;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class SimplePollAnswerComposer extends MessageComposer {
    private final int userId;
    private final String choice;
    private final int no;
    private final int yes;

    public SimplePollAnswerComposer(int userId, String choice, int no, int yes) {
        this.userId = userId;
        this.choice = choice;
        this.no = no;
        this.yes = yes;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.SimplePollAnswerComposer);
        this.response.appendInt(this.userId);
        this.response.appendString(this.choice);
        this.response.appendInt(2);
        this.response.appendString("0");
        this.response.appendInt(this.no);
        this.response.appendString("1");
        this.response.appendInt(this.yes);
        return this.response;
    }
}