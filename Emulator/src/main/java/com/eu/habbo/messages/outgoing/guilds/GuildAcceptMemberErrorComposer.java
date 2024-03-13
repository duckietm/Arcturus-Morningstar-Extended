package com.eu.habbo.messages.outgoing.guilds;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuildAcceptMemberErrorComposer extends MessageComposer {
    public final static int NO_LONGER_MEMBER = 0;
    public final static int ALREADY_REJECTED = 1;
    public final static int ALREADY_ACCEPTED = 2;

    private final int guildId;
    private final int errorCode;

    public GuildAcceptMemberErrorComposer(int guildId, int errorCode) {
        this.guildId = guildId;
        this.errorCode = errorCode;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuildAcceptMemberErrorComposer);
        this.response.appendInt(this.guildId);
        this.response.appendInt(this.errorCode);
        return this.response;
    }
}