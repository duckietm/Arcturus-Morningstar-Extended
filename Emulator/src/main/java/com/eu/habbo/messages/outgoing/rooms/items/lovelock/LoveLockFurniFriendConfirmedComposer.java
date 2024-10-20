package com.eu.habbo.messages.outgoing.rooms.items.lovelock;

import com.eu.habbo.habbohotel.items.interactions.InteractionLoveLock;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class LoveLockFurniFriendConfirmedComposer extends MessageComposer {
    private final InteractionLoveLock loveLock;

    public LoveLockFurniFriendConfirmedComposer(InteractionLoveLock loveLock) {
        this.loveLock = loveLock;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.LoveLockFurniFriendConfirmedComposer);
        this.response.appendInt(this.loveLock.getId());
        return this.response;
    }

    public InteractionLoveLock getLoveLock() {
        return loveLock;
    }
}
