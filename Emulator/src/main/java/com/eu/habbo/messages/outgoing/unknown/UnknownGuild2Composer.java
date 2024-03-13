package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UnknownGuild2Composer extends MessageComposer {
    private final int guildId;

    public UnknownGuild2Composer(int guildId) {
        this.guildId = guildId;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UnknownGuild2Composer);
        this.response.appendInt(this.guildId);
        return this.response;
    }
}