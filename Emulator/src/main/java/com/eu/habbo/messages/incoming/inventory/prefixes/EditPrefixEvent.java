package com.eu.habbo.messages.incoming.inventory.prefixes;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.inventory.prefixes.PrefixReceivedComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EditPrefixEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditPrefixEvent.class);

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int prefixId = this.packet.readInt();
        String text = this.packet.readString();
        String color = this.packet.readString();
        String icon = this.packet.readString();
        String effect = this.packet.readString();

        Habbo habbo = this.client.getHabbo();

        if (habbo == null) return;

        UserPrefix prefix = habbo.getInventory().getPrefixesComponent().getPrefix(prefixId);

        if (prefix == null) return;

        // Load settings
        int maxLength = getSettingInt("max_length", 15);

        // Validate text
        text = text.trim();

        if (text.isEmpty() || text.length() > maxLength) {
            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "Prefix text is invalid or too long (max " + maxLength + " characters)."));
            return;
        }

        // Validate color (single hex or comma-separated multi hex for per-letter colors)
        String[] colorParts = color.split(",");
        for (String part : colorParts) {
            if (!part.matches("^#[0-9A-Fa-f]{6}$")) {
                this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "Invalid color format."));
                return;
            }
        }

        // Check blacklist
        if (isBlacklisted(text)) {
            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "This prefix contains a blocked word."));
            return;
        }

        // Validate icon (allow empty or known icon names)
        if (icon == null) icon = "";
        icon = icon.trim();

        // Validate effect
        if (effect == null) effect = "";
        effect = effect.trim();

        // Update prefix
        prefix.setText(text);
        prefix.setColor(color);
        prefix.setIcon(icon);
        prefix.setEffect(effect);
        prefix.needsUpdate(true);
        prefix.run();

        this.client.sendResponse(new PrefixReceivedComposer(prefix));
    }

    private int getSettingInt(String key, int defaultValue) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT `value` FROM custom_prefix_settings WHERE key_name = ?")) {
            statement.setString(1, key);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return Integer.parseInt(set.getString("value"));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            LOGGER.error("Error reading prefix setting: " + key, e);
        }
        return defaultValue;
    }

    private boolean isBlacklisted(String text) {
        String lowerText = text.toLowerCase();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT word FROM custom_prefix_blacklist")) {
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (lowerText.contains(set.getString("word").toLowerCase())) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error checking prefix blacklist", e);
        }
        return false;
    }
}
