package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserSavedSettingsEvent;

/** Official wired menu preferences (1226); the emulator keeps only the wired whisper switch. */
public class SaveWiredMenuSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client
                .getHabbo()
                .getHabboStats()
                .setWiredWhisperDisabled(UserPreferencePackets.parseWiredWhisperDisabled(this.packet));
        Emulator.getPluginManager().fireEvent(new UserSavedSettingsEvent(this.client.getHabbo()));
    }
}
