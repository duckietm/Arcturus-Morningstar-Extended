package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.habbohotel.users.HabboNavigatorWindowSettings;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class NewNavigatorSettingsComposer extends MessageComposer {
    private final HabboNavigatorWindowSettings windowSettings;

    public NewNavigatorSettingsComposer(HabboNavigatorWindowSettings windowSettings) {
        this.windowSettings = windowSettings;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.NewNavigatorSettingsComposer);
        this.response.appendInt(this.windowSettings.x);
        this.response.appendInt(this.windowSettings.y);
        this.response.appendInt(this.windowSettings.width);
        this.response.appendInt(this.windowSettings.height);
        this.response.appendBoolean(this.windowSettings.openSearches);
        this.response.appendInt(0);
        return this.response;
    }
}
