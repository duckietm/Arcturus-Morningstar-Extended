package com.eu.habbo.messages.outgoing.friends;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomInviteComposer extends MessageComposer {
    private final int userId;
    private final String message;

    public RoomInviteComposer(int userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomInviteComposer);
        this.response.appendInt(this.userId);
        this.response.appendString(this.message);
        return this.response;
    }
}
