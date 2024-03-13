package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class CompetitionEntrySubmitResultComposer extends MessageComposer {
    private final int unknownInt1;
    private final String unknownString1;
    private final int result;
    private final List<String> unknownStringList1;
    private final List<String> unknownStringList2;

    public CompetitionEntrySubmitResultComposer(int unknownInt1, String unknownString1, int result, List<String> unknownStringList1, List<String> unknownStringList2) {
        this.unknownInt1 = unknownInt1;
        this.unknownString1 = unknownString1;
        this.result = result;
        this.unknownStringList1 = unknownStringList1;
        this.unknownStringList2 = unknownStringList2;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CompetitionEntrySubmitResultComposer);
        this.response.appendInt(this.unknownInt1);
        this.response.appendString(this.unknownString1);
        this.response.appendInt(this.result);
        this.response.appendInt(this.unknownStringList1.size());
        for (String s : this.unknownStringList1) {
            this.response.appendString(s);
        }

        this.response.appendInt(this.unknownStringList2.size());
        for (String s : this.unknownStringList2) {
            this.response.appendString(s);
        }
        return this.response;
    }
}