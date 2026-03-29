package com.eu.habbo.messages.incoming.uisettings;

import com.eu.habbo.messages.incoming.MessageHandler;

public class UiSettingsSaveEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        String settingsJson = this.packet.readString();

        // Basic validation: ensure it's valid JSON and not too large (max 10KB)
        if (settingsJson == null || settingsJson.length() > 10240) {
            return;
        }

        try {
            // Validate it's parseable JSON
            com.google.gson.JsonParser.parseString(settingsJson);
        } catch (Exception e) {
            return;
        }

        // Store in HabboInfo (persisted to DB in HabboInfo.run())
        this.client.getHabbo().getHabboInfo().setUiSettings(settingsJson);
    }
}
