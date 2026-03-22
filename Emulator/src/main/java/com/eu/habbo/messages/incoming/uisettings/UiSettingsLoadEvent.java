package com.eu.habbo.messages.incoming.uisettings;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.uisettings.UiSettingsDataComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UiSettingsLoadEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(UiSettingsLoadEvent.class);

    @Override
    public void handle() throws Exception {
        int userId = this.client.getHabbo().getHabboInfo().getId();

        LOGGER.info("[UiSettingsLoad] Loading settings for user {} (id: {})", this.client.getHabbo().getHabboInfo().getUsername(), userId);

        String settingsJson = "{}";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement("SELECT settings_json FROM users_ui_settings WHERE user_id = ?")) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        settingsJson = rs.getString("settings_json");
                    }
                }
            }
        }

        this.client.sendResponse(new UiSettingsDataComposer(settingsJson));
    }
}
