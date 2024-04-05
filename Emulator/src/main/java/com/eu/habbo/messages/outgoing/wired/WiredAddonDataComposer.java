package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class WiredAddonDataComposer extends MessageComposer {
    private final InteractionWiredExtra addon;
    private final Room room;

    public WiredAddonDataComposer(InteractionWiredExtra addon, Room room) {
        this.addon = addon;
        this.room = room;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredAddonDataComposer);
        this.addon.serializeWiredData(this.response, this.room);
        this.addon.needsUpdate(true);
        return this.response;
    }
}
