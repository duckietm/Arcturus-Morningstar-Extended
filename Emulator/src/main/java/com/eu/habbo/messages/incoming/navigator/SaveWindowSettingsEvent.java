package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.habbohotel.users.HabboNavigatorWindowSettings;
import com.eu.habbo.messages.incoming.MessageHandler;

public class SaveWindowSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        HabboNavigatorWindowSettings windowSettings = this.client.getHabbo().getHabboStats().navigatorWindowSettings;

        windowSettings.x = this.packet.readInt();
        windowSettings.y = this.packet.readInt();

        windowSettings.width = this.packet.readInt();
        windowSettings.height = this.packet.readInt();

        boolean openSearches = this.packet.readBoolean();
        windowSettings.openSearches = openSearches;

        int unknownVar = this.packet.readInt();
        int unknown = unknownVar;
    }
}
