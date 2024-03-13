package com.eu.habbo.messages.incoming.friends;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.friends.LoadFriendRequestsComposer;

public class RequestFriendRequestsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new LoadFriendRequestsComposer(this.client.getHabbo()));
    }
}
