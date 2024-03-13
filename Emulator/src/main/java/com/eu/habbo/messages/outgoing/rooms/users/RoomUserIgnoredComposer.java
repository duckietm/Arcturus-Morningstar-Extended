package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomUserIgnoredComposer extends MessageComposer {
    public final static int IGNORED = 1;
    public final static int MUTED = 2;
    public final static int UNIGNORED = 3;

    private final Habbo habbo;
    private final int state;

    public RoomUserIgnoredComposer(Habbo habbo, int state) {
        this.habbo = habbo;
        this.state = state;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUserIgnoredComposer);
        this.response.appendInt(this.state);
        this.response.appendString(this.habbo.getHabboInfo().getUsername());
        return this.response;
    }
}
