package com.eu.habbo.messages.outgoing.guardians;

import com.eu.habbo.habbohotel.guides.GuardianTicket;
import com.eu.habbo.habbohotel.guides.GuardianVote;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Map;

public class GuardianVotingResultComposer extends MessageComposer {
    private final GuardianTicket ticket;
    private final GuardianVote vote;

    public GuardianVotingResultComposer(GuardianTicket ticket, GuardianVote vote) {
        this.ticket = ticket;
        this.vote = vote;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuardianVotingResultComposer);
        this.response.appendInt(this.ticket.getVerdict().getType()); //Final Verdict
        this.response.appendInt(this.vote.type.getType()); //Your vote

        this.response.appendInt(this.ticket.getVotes().size() - 1); //Other votes count.

        for (Map.Entry<Habbo, GuardianVote> set : this.ticket.getVotes().entrySet()) {
            if (set.getValue().equals(this.vote))
                continue;

            this.response.appendInt(set.getValue().type.getType());
        }
        return this.response;
    }
}
