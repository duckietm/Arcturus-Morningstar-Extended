package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AddUserBadgeComposer extends MessageComposer {
    private final HabboBadge badge;
    private final String senderName;

    public AddUserBadgeComposer(HabboBadge badge) {
        this(badge, "");
    }

    public AddUserBadgeComposer(HabboBadge badge, String senderName) {
        this.badge = badge;
        this.senderName = senderName == null ? "" : senderName;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.AddUserBadgeComposer);
        this.response.appendInt(this.badge.getId());
        this.response.appendString(this.badge.getCode());
        this.response.appendString(this.senderName);
        return this.response;
    }

    public HabboBadge getBadge() {
        return badge;
    }

    public String getSenderName() {
        return senderName;
    }
}
