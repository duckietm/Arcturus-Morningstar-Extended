package com.eu.habbo.messages.outgoing.rooms.users;

import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomUserWhisperComposer extends MessageComposer {
    private final RoomChatMessage roomChatMessage;

    public RoomUserWhisperComposer(RoomChatMessage roomChatMessage) {
        this.roomChatMessage = roomChatMessage;
    }

    @Override
    protected ServerMessage composeInternal() {
        if (this.roomChatMessage.getMessage().isEmpty())
            return null;

        this.response.init(Outgoing.RoomUserWhisperComposer);
        this.roomChatMessage.serialize(this.response);

        return this.response;
    }
}
