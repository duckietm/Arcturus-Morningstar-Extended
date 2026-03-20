package com.eu.habbo.messages.incoming.inventory.prefixes;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.UserPrefix;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.inventory.prefixes.PrefixReceivedComposer;
import com.eu.habbo.messages.outgoing.users.UserCreditsComposer;
import com.eu.habbo.messages.outgoing.users.UserCurrencyComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PurchasePrefixEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurchasePrefixEvent.class);

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        String text = this.packet.readString();
        String color = this.packet.readString();
        String icon = this.packet.readString();
        String effect = this.packet.readString();

        Habbo habbo = this.client.getHabbo();

        if (habbo == null) return;

        // Load settings
        int maxLength = getSettingInt("max_length", 15);
        int minRank = getSettingInt("min_rank_to_buy", 1);
        int priceCredits = getSettingInt("price_credits", 5);
        int pricePoints = getSettingInt("price_points", 0);
        int pointsType = getSettingInt("points_type", 0);

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

        // Check rank
        if (habbo.getHabboInfo().getRank().getId() < minRank) {
            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "Your rank is too low to purchase prefixes."));
            return;
        }

        // Check blacklist
        if (isBlacklisted(text)) {
            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "This prefix contains a blocked word."));
            return;
        }

        // Check credits
        if (priceCredits > 0 && habbo.getHabboInfo().getCredits() < priceCredits) {
            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "Not enough credits."));
            return;
        }

        // Check points
        if (pricePoints > 0 && habbo.getHabboInfo().getCurrencyAmount(pointsType) < pricePoints) {
            this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "Not enough points."));
            return;
        }

        // Deduct currency
        if (priceCredits > 0) {
            habbo.getHabboInfo().addCredits(-priceCredits);
            this.client.sendResponse(new UserCreditsComposer(habbo));
        }

        if (pricePoints > 0) {
            habbo.getHabboInfo().addCurrencyAmount(pointsType, -pricePoints);
            this.client.sendResponse(new UserCurrencyComposer(habbo));
        }

        // Validate icon (allow empty or known icon names)
        if (icon == null) icon = "";
        icon = icon.trim();

        // Validate effect
        if (effect == null) effect = "";
        effect = effect.trim();

        // Create prefix
        UserPrefix prefix = new UserPrefix(habbo.getHabboInfo().getId(), text, color, icon, effect);
        prefix.run(); // Insert into DB synchronously to get the ID
        habbo.getInventory().getPrefixesComponent().addPrefix(prefix);

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
