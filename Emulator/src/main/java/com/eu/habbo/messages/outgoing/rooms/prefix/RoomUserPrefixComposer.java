package com.eu.habbo.messages.outgoing.rooms.prefix;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class RoomUserPrefixComposer extends MessageComposer {
    private final int userId;
    private final int prefixId;
    private final String text;
    private final String color;
    private final String icon;
    private final String effect;

    public RoomUserPrefixComposer(int userId, int prefixId, String text, String color, String icon, String effect) {
        this.userId = userId;
        this.prefixId = prefixId;
        this.text = text;
        this.color = color;
        this.icon = icon;
        this.effect = effect;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomUserPrefixComposer);
        this.response.appendInt(this.userId);
        this.response.appendInt(this.prefixId);
        this.response.appendString(this.text);
        this.response.appendString(this.color);
        this.response.appendString(this.icon);
        this.response.appendString(this.effect);
        return this.response;
    }
}
