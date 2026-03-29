package com.eu.habbo.messages.incoming.uisettings;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.uisettings.UiSettingsDataComposer;

public class UiSettingsLoadEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        String uiSettings = this.client.getHabbo().getHabboInfo().getUiSettings();
        this.client.sendResponse(new UiSettingsDataComposer(uiSettings));
    }
}
