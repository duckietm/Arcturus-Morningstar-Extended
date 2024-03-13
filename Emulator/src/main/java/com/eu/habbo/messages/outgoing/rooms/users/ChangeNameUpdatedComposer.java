package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ChangeNameUpdatedComposer extends MessageComposer {
    private final Habbo habbo;

    public ChangeNameUpdatedComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ChangeNameUpdateComposer);
        this.response.appendInt(0);
        this.response.appendString(this.habbo.getHabboInfo().getUsername());
        this.response.appendInt(0);
        return this.response;
    }
}
