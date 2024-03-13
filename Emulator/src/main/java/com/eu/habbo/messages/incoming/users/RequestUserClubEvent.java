package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.UserClubComposer;

public class RequestUserClubEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String subscriptionType = this.packet.readString();
        this.client.sendResponse(new UserClubComposer(this.client.getHabbo(), subscriptionType));
    }
}
