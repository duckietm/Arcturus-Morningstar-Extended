package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ConvertedForwardToRoomComposer extends MessageComposer {
    private final String unknownString1;
    private final int unknownInt1;

    public ConvertedForwardToRoomComposer(String unknownString1, int unknownInt1) {
        this.unknownString1 = unknownString1;
        this.unknownInt1 = unknownInt1;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ConvertedForwardToRoomComposer);
        this.response.appendString(this.unknownString1);
        this.response.appendInt(this.unknownInt1);
        return this.response;
    }
}