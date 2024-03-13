package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UserPermissionsComposer extends MessageComposer {
    private final int clubLevel;

    private final Habbo habbo;

    public UserPermissionsComposer(Habbo habbo) {
        this.clubLevel = habbo.getHabboStats().hasActiveClub() ? 2 : 0;
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserPermissionsComposer);
        this.response.appendInt(this.clubLevel);
        this.response.appendInt(this.habbo.getHabboInfo().getRank().getLevel());
        this.response.appendBoolean(this.habbo.hasPermission(Permission.ACC_AMBASSADOR));
        return this.response;
    }
}
