package com.eu.habbo.messages.outgoing.friends;

import com.eu.habbo.habbohotel.messenger.FriendRequest;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class LoadFriendRequestsComposer extends MessageComposer {
    private final Habbo habbo;

    public LoadFriendRequestsComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.LoadFriendRequestsComposer);

        synchronized (this.habbo.getMessenger().getFriendRequests()) {
            this.response.appendInt(this.habbo.getMessenger().getFriendRequests().size());
            this.response.appendInt(this.habbo.getMessenger().getFriendRequests().size());

            for (FriendRequest friendRequest : this.habbo.getMessenger().getFriendRequests()) {
                this.response.appendInt(friendRequest.getId());
                this.response.appendString(friendRequest.getUsername());
                this.response.appendString(friendRequest.getLook());
            }
        }

        return this.response;
    }
}