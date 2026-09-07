package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserSavedSettingsEvent;

/** Official SetChatPreferences (2506): chat mode, bubble width and scroll speed of the user. */
public class SaveChatPreferencesEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        UserPreferencePackets.ChatPreferences preferences = UserPreferencePackets.parseChatPreferences(this.packet);
        this.client
                .getHabbo()
                .getHabboStats()
                .setChatPreferences(
                        preferences.chatMode(), preferences.chatBubbleWidth(), preferences.chatScrollSpeed());
        Emulator.getPluginManager().fireEvent(new UserSavedSettingsEvent(this.client.getHabbo()));
    }
}
