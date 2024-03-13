package com.eu.habbo.messages.outgoing.friends;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import gnu.trove.list.array.TIntArrayList;

public class RemoveFriendComposer extends MessageComposer {
    private final TIntArrayList unfriendIds;

    public RemoveFriendComposer(TIntArrayList unfriendIds) {
        this.unfriendIds = unfriendIds;
    }

    public RemoveFriendComposer(int i) {
        this.unfriendIds = new TIntArrayList();
        this.unfriendIds.add(i);
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UpdateFriendComposer);

        this.response.appendInt(0);
        this.response.appendInt(this.unfriendIds.size());
        for (int i = 0; i < this.unfriendIds.size(); i++) {
            this.response.appendInt(-1);
            this.response.appendInt(this.unfriendIds.get(i));
        }

        return this.response;
    }
}
