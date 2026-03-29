package com.eu.habbo.messages.outgoing.uisettings;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class UiSettingsDataComposer extends MessageComposer {
    private final String settingsJson;

    public UiSettingsDataComposer(String settingsJson) {
        this.settingsJson = settingsJson != null ? settingsJson : "{}";
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.UiSettingsDataComposer);
        this.response.appendString(this.settingsJson);
        return this.response;
    }
}
