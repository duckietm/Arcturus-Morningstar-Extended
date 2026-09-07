package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.incoming.MessageHandler;

/** Official wired menu preferences (1226); the emulator keeps only the wired whisper switch. */
public class SaveWiredMenuSettingsEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        this.client
                .getHabbo()
                .getHabboStats()
                .setWiredWhisperDisabled(UserPreferencePackets.parseWiredWhisperDisabled(this.packet));
    }
}
