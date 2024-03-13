package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.MeMenuSettingsComposer;

public class RequestMeMenuSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.sendResponse(new MeMenuSettingsComposer(this.client.getHabbo()));
    }
}
