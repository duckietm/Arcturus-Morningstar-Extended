package com.eu.habbo.messages.outgoing.modtool;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ModToolIssueHandlerDimensionsComposer extends MessageComposer {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public ModToolIssueHandlerDimensionsComposer(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ModToolIssueHandlerDimensionsComposer);
        this.response.appendInt(this.x);
        this.response.appendInt(this.y);
        this.response.appendInt(this.width);
        this.response.appendInt(this.height);
        return this.response;
    }
}