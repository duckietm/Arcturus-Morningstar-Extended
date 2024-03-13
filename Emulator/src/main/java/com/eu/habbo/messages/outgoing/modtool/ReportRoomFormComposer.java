package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.habbohotel.modtool.ModToolIssue;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class ReportRoomFormComposer extends MessageComposer {
    private final List<ModToolIssue> pendingIssues;

    public ReportRoomFormComposer(List<ModToolIssue> issues) {
        this.pendingIssues = issues;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ReportRoomFormComposer);
        this.response.appendInt(this.pendingIssues.size()); //Current standing help request(s) amount:

        for (ModToolIssue issue : this.pendingIssues) {
            this.response.appendString(issue.id + "");
            this.response.appendString(issue.timestamp + "");
            this.response.appendString(issue.message);
        }
        return this.response;
    }
}
