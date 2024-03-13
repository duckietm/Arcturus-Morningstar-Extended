package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Map;

public class UnknownRoomDesktopComposer extends MessageComposer {
    private final int unknownInt1;
    private final Map<Integer, String> unknownMap;

    public UnknownRoomDesktopComposer(int unknownInt1, Map<Integer, String> unknownMap) {
        this.unknownInt1 = unknownInt1;
        this.unknownMap = unknownMap;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UnknownRoomDesktopComposer);
        this.response.appendInt(this.unknownInt1);
        this.response.appendInt(this.unknownMap.size());
        for (Map.Entry<Integer, String> entry : this.unknownMap.entrySet()) {
            this.response.appendInt(entry.getKey());
            this.response.appendString(entry.getValue());
        }
        return this.response;
    }
}