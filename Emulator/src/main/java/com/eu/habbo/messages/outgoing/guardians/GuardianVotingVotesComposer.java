package com.eu.habbo.messages.outgoing.guardians;

import com.eu.habbo.habbohotel.guides.GuardianTicket;
import com.eu.habbo.habbohotel.guides.GuardianVote;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;

public class GuardianVotingVotesComposer extends MessageComposer {
    private final GuardianTicket ticket;
    private final Habbo guardian;

    public GuardianVotingVotesComposer(GuardianTicket ticket, Habbo guardian) {
        this.ticket = ticket;
        this.guardian = guardian;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuardianVotingVotesComposer);

        ArrayList<GuardianVote> votes = this.ticket.getSortedVotes(this.guardian);

        this.response.appendInt(votes.size());

        for (GuardianVote vote : votes) {
            this.response.appendInt(vote.type.getType());
        }

        return this.response;
    }
}
