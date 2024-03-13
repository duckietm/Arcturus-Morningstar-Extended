package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AddUserBadgeComposer extends MessageComposer {
    private final HabboBadge badge;

    public AddUserBadgeComposer(HabboBadge badge) {
        this.badge = badge;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AddUserBadgeComposer);
        this.response.appendInt(this.badge.getId());
        this.response.appendString(this.badge.getCode());
        return this.response;
    }
}
