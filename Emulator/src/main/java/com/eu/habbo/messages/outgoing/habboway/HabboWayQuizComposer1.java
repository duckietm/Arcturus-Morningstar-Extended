package com.eu.habbo.messages.outgoing.habboway;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class HabboWayQuizComposer1 extends MessageComposer {
    public final String name;
    public final int[] items;

    public HabboWayQuizComposer1(String name, int[] items) {
        this.name = name;
        this.items = items;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.HabboWayQuizComposer1);
        this.response.appendString(this.name);
        this.response.appendInt(this.items.length);
        for (int item : this.items) {
            this.response.appendInt(item);
        }
        return this.response;
    }
}