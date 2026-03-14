package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserSavedSettingsEvent;

public class SaveBlockCameraFollowEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client.getHabbo().getHabboStats().blockCameraFollow = this.packet.readBoolean();
        Emulator.getPluginManager().fireEvent(new UserSavedSettingsEvent(this.client.getHabbo()));
    }
}
