package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomPaintComposer extends MessageComposer {
    private final String type;
    private final String value;

    public RoomPaintComposer(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomPaintComposer);
        this.response.appendString(this.type);
        this.response.appendString(this.value);
        return this.response;
    }
}
