package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ItemIntStateComposer extends MessageComposer {
    private final int id;
    private final int value;

    public ItemIntStateComposer(int id, int value) {
        this.id = id;
        this.value = value;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ItemStateComposer2);
        this.response.appendInt(this.id);
        this.response.appendInt(this.value);
        return this.response;
    }
}