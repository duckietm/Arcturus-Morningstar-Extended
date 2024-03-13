package com.eu.habbo.messages.incoming.unknown;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.unknown.IgnoredUsersComposer;

public class UnknownEvent1 extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new IgnoredUsersComposer());
    }
}
