package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Free custom colours for recolourable furni.
 *
 * <p>A furni is tintable when its asset declares the {@code furniture_guild_customized} logic and carries layers
 * tagged COLOR1 / COLOR2: the renderer paints those layers with the two colours it receives in the 5-string stuff
 * data (state, guild id, badge, colour 1, colour 2). Habbo only ever sent that block for guild furni, so the colour
 * support used to live in {@link InteractionGuildFurni} alone — which left every tintable furni whose interaction is
 * something else (gates, rollers, teleports, vending machines, multiheight blocks, switches, ...) permanently grey.
 *
 * <p>The colours are stored per item in {@code items.wired_data}, a column plain furni never use, and are serialized
 * for any interaction through {@link HabboItem#serializeItemData(ServerMessage)}.
 */
public final class FurnitureCustomColors {
    private static final Logger LOGGER = LoggerFactory.getLogger(FurnitureCustomColors.class);
    private static final String COLORABLE_PARAM = "colorable";

    private FurnitureCustomColors() {}

    /** True when the base item is flagged recolourable ({@code items_base.customparams} contains "colorable"). */
    public static boolean isColorable(Item baseItem) {
        if (baseItem == null) return false;
        String params = baseItem.getCustomParams();
        return params != null && params.toLowerCase(Locale.ROOT).contains(COLORABLE_PARAM);
    }

    /** Six hex digits without '#', or an empty string when the value is not a colour. */
    public static String normalizeColor(String value) {
        if (value == null) return "";
        String hex = value.trim().toUpperCase(Locale.ROOT);
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.startsWith("0X")) hex = hex.substring(2);
        return hex.matches("[0-9A-F]{6}") ? hex : "";
    }

    public static String colorToHex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    /**
     * Reads the two colours out of a {@code wired_data} payload. Returns {@code null} when the payload is not ours
     * (a real wired item's data, or anything unparseable), so wired furni keep their own JSON untouched.
     */
    public static String[] parse(String wiredData) {
        if (wiredData == null || wiredData.isBlank() || !wiredData.trim().startsWith("{")) return null;

        try {
            JsonObject json = JsonParser.parseString(wiredData).getAsJsonObject();
            if (!json.has("colorOne")) return null;

            String one = normalizeColor(json.get("colorOne").getAsString());
            if (one.isEmpty()) return null;

            String two = json.has("colorTwo") ? normalizeColor(json.get("colorTwo").getAsString()) : "";
            return new String[] {one, two.isEmpty() ? one : two};
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static String payload(String colorOne, String colorTwo) {
        String one = normalizeColor(colorOne);
        if (one.isEmpty()) return "";

        String two = normalizeColor(colorTwo);
        return "{\"colorOne\":\"" + one + "\",\"colorTwo\":\"" + (two.isEmpty() ? one : two) + "\"}";
    }

    /**
     * The two colours a recolourable-page purchase carries, as "RRGGBB,RRGGBB" (a single colour is allowed and is
     * used for both layers). Returns {@code null} when the extra data is not a colour pair, so every other purchase
     * keeps its normal meaning.
     */
    public static String[] parsePurchaseColors(String extraData) {
        if (extraData == null || extraData.isBlank()) return null;

        String[] parts = extraData.trim().split(",", 3);
        if (parts.length > 2) return null;

        String one = normalizeColor(parts[0]);
        if (one.isEmpty()) return null;

        String two = parts.length > 1 ? normalizeColor(parts[1]) : "";
        return new String[] {one, two.isEmpty() ? one : two};
    }

    /** Persists the colours of one item. An empty {@code colorOne} clears them. */
    public static boolean save(int itemId, String colorOne, String colorTwo) {
        String payload = payload(colorOne, colorTwo);

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("UPDATE items SET wired_data = ? WHERE id = ?")) {
            statement.setString(1, payload);
            statement.setInt(2, itemId);
            statement.execute();
            return true;
        } catch (SQLException exception) {
            LOGGER.error("Could not save custom colours of item {}", itemId, exception);
            return false;
        }
    }

    /**
     * Writes the guild-customised stuff data (5 strings) a tintable furni needs. Guild id 0 and an empty badge mean
     * "no guild, just colours" — exactly what {@link FurnitureGuildCustomizedLogic} on the client expects.
     */
    public static void serialize(HabboItem item, ServerMessage message) {
        message.appendInt(2 + (item.isLimited() ? 256 : 0));
        message.appendInt(5);
        message.appendString(item.getExtradata());
        message.appendString("0");
        message.appendString("");
        message.appendString(item.getCustomColorOne());
        message.appendString(item.getCustomColorTwo());

        if (item.isLimited()) {
            message.appendInt(item.getLimitedSells());
            message.appendInt(item.getLimitedStack());
        }
    }
}
