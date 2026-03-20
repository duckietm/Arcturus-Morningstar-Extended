package com.eu.habbo.messages.incoming.inventory.prefixes;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.inventory.prefixes.UserPrefixesComposer;

public class RequestUserPrefixesEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new UserPrefixesComposer(this.client.getHabbo()));
    }
}
