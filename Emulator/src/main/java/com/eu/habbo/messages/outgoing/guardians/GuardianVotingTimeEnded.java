package com.eu.habbo.messages.outgoing.guardians;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuardianVotingTimeEnded extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuardianVotingTimeEnded);
        return this.response;
    }
}
