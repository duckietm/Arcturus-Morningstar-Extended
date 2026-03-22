package com.eu.habbo.messages.incoming.uisettings;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UiSettingsSaveEvent extends MessageHandler {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(UiSettingsSaveEvent.class);

    @Override
    public void handle() throws Exception {
        String settingsJson = this.packet.readString();
        int userId = this.client.getHabbo().getHabboInfo().getId();

        LOGGER.info("[UiSettingsSave] Saving settings for user {} (id: {})", this.client.getHabbo().getHabboInfo().getUsername(), userId);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users_ui_settings (user_id, settings_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE settings_json = ?")) {
                stmt.setInt(1, userId);
                stmt.setString(2, settingsJson);
                stmt.setString(3, settingsJson);
                stmt.executeUpdate();
            }
        }
    }
}
