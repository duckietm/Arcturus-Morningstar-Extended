package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;

public class UserBadgesComposer extends MessageComposer {
    private final ArrayList<HabboBadge> badges;
    private final int habbo;

    public UserBadgesComposer(ArrayList<HabboBadge> badges, int habbo) {
        this.badges = badges;
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserBadgesComposer);
        this.response.appendInt(this.habbo);
        synchronized (this.badges) {
            this.response.appendInt(this.badges.size());
            for (HabboBadge badge : this.badges) {
                this.response.appendInt(badge.getSlot());
                this.response.appendString(badge.getCode());
            }
        }
        return this.response;
    }
}
