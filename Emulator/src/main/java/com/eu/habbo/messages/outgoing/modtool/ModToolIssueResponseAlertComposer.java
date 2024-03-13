package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ModToolIssueResponseAlertComposer extends MessageComposer {
    private final String message;

    public ModToolIssueResponseAlertComposer(String message) {
        this.message = message;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolIssueResponseAlertComposer);
        this.response.appendString(this.message);
        return this.response;
    }
}
