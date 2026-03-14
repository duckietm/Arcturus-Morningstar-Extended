package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.navigator.NewNavigatorSettingsComposer;

public class RequestNavigatorSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {


        this.client.sendResponse(new NewNavigatorSettingsComposer(this.client.getHabbo().getHabboStats().navigatorWindowSettings));

    }
}
