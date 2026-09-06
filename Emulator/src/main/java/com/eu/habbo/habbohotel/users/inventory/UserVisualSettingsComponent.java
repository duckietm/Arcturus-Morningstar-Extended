package com.eu.habbo.habbohotel.users.inventory;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class UserVisualSettingsComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserVisualSettingsComponent.class);
    public static final String DEFAULT_DISPLAY_ORDER = "icon-prefix-name";
    private static final Set<String> ALLOWED_PARTS = new HashSet<>(Arrays.asList("icon", "prefix", "name"));

    private final Habbo habbo;
    private String displayOrder = DEFAULT_DISPLAY_ORDER;
    /** Username colour as "#RRGGBB", or "" for the default (white). */
    private String nameColor = "";

    public UserVisualSettingsComponent(Habbo habbo) {
        this.habbo = habbo;
        this.loadSettings();
    }

    private void loadSettings() {
        int userId = this.habbo.getHabboInfo().getId();
        this.displayOrder = loadDisplayOrder(userId);
        this.nameColor = loadNameColor(userId);
    }

    public String getDisplayOrder() {
        return sanitizeDisplayOrder(this.displayOrder);
    }

    public void setDisplayOrder(String displayOrder) {
        this.displayOrder = sanitizeDisplayOrder(displayOrder);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO user_visual_settings (user_id, display_order) VALUES (?, ?) ON DUPLICATE KEY UPDATE display_order = VALUES(display_order)")) {
            statement.setInt(1, this.habbo.getHabboInfo().getId());
            statement.setString(2, this.displayOrder);
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while saving user visual settings", e);
        }
    }

    public String getNameColor() {
        return sanitizeNameColor(this.nameColor);
    }

    public void setNameColor(String nameColor) {
        this.nameColor = sanitizeNameColor(nameColor);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO user_visual_settings (user_id, display_order, name_color) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name_color = VALUES(name_color)")) {
            statement.setInt(1, this.habbo.getHabboInfo().getId());
            statement.setString(2, this.getDisplayOrder());
            statement.setString(3, this.nameColor);
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while saving user name colour", e);
        }
    }

    public static String loadDisplayOrder(int userId) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT display_order FROM user_visual_settings WHERE user_id = ? LIMIT 1")) {
            statement.setInt(1, userId);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return sanitizeDisplayOrder(set.getString("display_order"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while loading user visual settings", e);
        }

        return DEFAULT_DISPLAY_ORDER;
    }

    public static String loadNameColor(int userId) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT name_color FROM user_visual_settings WHERE user_id = ? LIMIT 1")) {
            statement.setInt(1, userId);

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return sanitizeNameColor(set.getString("name_color"));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception while loading user name colour", e);
        }

        return "";
    }

    /** Accepts "#RRGGBB" / "RRGGBB" (case-insensitive); anything else clears the colour. */
    public static String sanitizeNameColor(String nameColor) {
        if (nameColor == null) return "";
        String value = nameColor.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (!value.matches("(?i)[0-9a-f]{6}")) return "";
        return "#" + value.toUpperCase(Locale.ROOT);
    }

    public static String sanitizeDisplayOrder(String displayOrder) {
        if (displayOrder == null || displayOrder.trim().isEmpty()) {
            return DEFAULT_DISPLAY_ORDER;
        }

        String[] parts = displayOrder.trim().toLowerCase().split("-");

        if (parts.length != 3) {
            return DEFAULT_DISPLAY_ORDER;
        }

        Set<String> uniqueParts = new HashSet<>();

        for (String part : parts) {
            if (!ALLOWED_PARTS.contains(part) || !uniqueParts.add(part)) {
                return DEFAULT_DISPLAY_ORDER;
            }
        }

        return String.join("-", parts);
    }

    public void dispose() {
        this.displayOrder = DEFAULT_DISPLAY_ORDER;
        this.nameColor = "";
    }
}
