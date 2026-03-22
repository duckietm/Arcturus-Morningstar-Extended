package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class WiredExtraDataComposer extends MessageComposer {
    private final InteractionWiredExtra extra;
    private final Room room;

    public WiredExtraDataComposer(InteractionWiredExtra extra, Room room) {
        this.extra = extra;
        this.room = room;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredEffectDataComposer);
        this.extra.serializeWiredData(this.response, this.room);
        this.extra.needsUpdate(true);
        return this.response;
    }
}
