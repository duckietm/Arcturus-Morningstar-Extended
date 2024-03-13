package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomAdErrorComposer extends MessageComposer {
    private final int errorCode;
    private final String unknownString;

    public RoomAdErrorComposer(int errorCode, String unknownString) {
        this.errorCode = errorCode;
        this.unknownString = unknownString;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomAdErrorComposer);
        this.response.appendInt(this.errorCode);
        this.response.appendString(this.unknownString);
        return this.response;
    }
}