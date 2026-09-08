package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserSavedSettingsEvent;

/** Official SetOnlineIndicatorPreference (818): which friends trigger the "came online" bubble. */
public class SaveOnlineIndicatorPreferenceEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client
                .getHabbo()
                .getHabboStats()
                .setOnlineIndicatorPreference(UserPreferencePackets.parseOnlineIndicatorPreference(this.packet));
        Emulator.getPluginManager().fireEvent(new UserSavedSettingsEvent(this.client.getHabbo()));
    }
}
