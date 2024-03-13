package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UserHomeRoomComposer extends MessageComposer {
    private final int homeRoom;
    private final int roomToEnter;

    public UserHomeRoomComposer(int homeRoom, int roomToEnter) {
        this.homeRoom = homeRoom;
        this.roomToEnter = roomToEnter;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UserHomeRoomComposer);
        this.response.appendInt(this.homeRoom);
        this.response.appendInt(this.roomToEnter);
        return this.response;
    }
}
