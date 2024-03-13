package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class TalentTrackEmailVerifiedComposer extends MessageComposer {
    private final String email;
    private final boolean unknownB1;
    private final boolean unknownB2;

    public TalentTrackEmailVerifiedComposer(String email, boolean unknownB1, boolean unknownB2) {
        this.email = email;
        this.unknownB1 = unknownB1;
        this.unknownB2 = unknownB2;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.TalentTrackEmailVerifiedComposer);
        this.response.appendString(this.email);
        this.response.appendBoolean(this.unknownB1);
        this.response.appendBoolean(this.unknownB2);
        return this.response;
    }
}