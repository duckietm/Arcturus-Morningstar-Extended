package com.eu.habbo.messages.outgoing.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;

public class RoomInviteErrorComposer extends MessageComposer {
    private final int errorCode;
    private final THashSet<MessengerBuddy> buddies;

    public RoomInviteErrorComposer(int errorCode, THashSet<MessengerBuddy> buddies) {
        this.errorCode = errorCode;
        this.buddies = buddies;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.RoomInviteErrorComposer);
        this.response.appendInt(this.errorCode);
        this.response.appendInt(this.buddies.size());
        this.buddies.forEach(new TObjectProcedure<MessengerBuddy>() {
            @Override
            public boolean execute(MessengerBuddy object) {
                RoomInviteErrorComposer.this.response.appendInt(object.getId());
                return true;
            }
        });
        return this.response;
    }
}