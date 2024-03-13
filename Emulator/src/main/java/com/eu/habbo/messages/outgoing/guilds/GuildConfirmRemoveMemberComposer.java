package com.eu.habbo.messages.outgoing.guilds;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuildConfirmRemoveMemberComposer extends MessageComposer {
    private int userId;
    private int furniCount;

    public GuildConfirmRemoveMemberComposer(int userId, int furniCount) {
        this.userId = userId;
        this.furniCount = furniCount;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuildConfirmRemoveMemberComposer);
        this.response.appendInt(this.userId);
        this.response.appendInt(this.furniCount);
        return this.response;
    }
}
