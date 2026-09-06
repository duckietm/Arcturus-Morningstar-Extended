package com.eu.habbo.messages.outgoing.gamedata;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/** CUSTOM packet 10101: hotel-wide renderer settings (JSON), sent at login and broadcast when the staff changes them. */
public class ClientRenderSettingsComposer extends MessageComposer {
    private final String json;

    public ClientRenderSettingsComposer(String json) {
        this.json = json == null || json.isBlank() ? "{}" : json;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ClientRenderSettingsComposer);
        this.response.appendString(this.json);
        return this.response;
    }
}
