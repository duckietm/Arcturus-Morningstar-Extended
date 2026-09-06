package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.catalogadmin.CatalogAdminResultComposer;
import com.eu.habbo.messages.outgoing.gamedata.ClientRenderSettingsComposer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CUSTOM packet 10102: the staff rendering panel saves the HOTEL-WIDE renderer settings (canvas resolution
 * rule, texture sampling, pixel rounding, room boundary mask). The sanitized JSON is stored in
 * emulator_settings `client.render.settings`, sent to every client at login and broadcast at once on change.
 *
 * Payload: String json. Answers with a CatalogAdminResultComposer prefixed [RENDER_SETTINGS] on failure.
 */
public class ClientRenderSettingsSaveEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRenderSettingsSaveEvent.class);
    private static final String PREFIX = "[RENDER_SETTINGS] ";
    public static final String CONFIG_KEY = "client.render.settings";
    private static final String MIN_RANK_KEY = "client.render.settings.min_rank";
    // emulator_settings.value is a TEXT column (65535 bytes); stay well inside it so a save never half-lands.
    private static final int MAX_PAYLOAD = 32768;
    private static final Set<String> RESOLUTIONS = Set.of("auto", "native", "supersample", "integer", "1", "2", "3");
    private static final Set<String> SCALES = Set.of("auto", "nearest", "linear");
    private static final Set<String> SMOOTHINGS = Set.of("pixelated", "crisp-edges", "smooth");
    private static final Set<String> WRAPS = Set.of("clamp-to-edge", "repeat", "mirror-repeat");
    private static final Set<String> SPRITE_SNAPS = Set.of("round", "floor", "ceil", "none");
    private static final Set<String> OFFSET_SNAPS = Set.of("round", "floor", "none");
    private static final Set<String> ZOOM_SNAPS = Set.of("integer", "half", "quarter");
    private static final int MAX_SIT_DEPTH_OVERRIDES = 256;
    /**
     * Keys the emulator validates itself. Anything else the client sends is forwarded as-is when it is a simple
     * primitive (see sanitize): the client re-validates every key on receipt, and this keeps a new renderer knob
     * from needing an emulator rebuild and a restart that drops every player.
     */
    private static final Set<String> KNOWN_KEYS = Set.of(
            "resolution", "scale", "round", "mask", "seatAnchor", "maxResolution", "pixelated", "sitDepth",
            "altitudeDepth", "canvasSmoothing", "antialias", "backBuffer", "mipmap", "wrap", "anisotropy",
            "spriteRound", "offsetRound", "zoomSnap", "sitDepthByClass");

    @Override
    public void handle() throws Exception {
        String raw = this.packet.readString();
        Habbo habbo = this.client.getHabbo();

        if (habbo == null) return;

        int minRank = Emulator.getConfig().getInt(MIN_RANK_KEY, 7);
        if (habbo.getHabboInfo().getRank().getLevel() < minRank && !habbo.hasPermission("acc_supporttool")) {
            this.reply(false, "Solo lo staff (rank " + minRank + "+) può cambiare il rendering del hotel");
            return;
        }

        if (raw == null || raw.length() > MAX_PAYLOAD) {
            this.reply(false, "Impostazioni non valide");
            return;
        }

        String sanitized = sanitize(raw);
        if (sanitized == null) {
            this.reply(false, "Impostazioni non valide");
            return;
        }

        if (!persist(sanitized)) {
            this.reply(false, "Errore database");
            return;
        }

        Emulator.getConfig().update(CONFIG_KEY, sanitized);
        LOGGER.info("Hotel render settings changed by {}: {}", habbo.getHabboInfo().getUsername(), sanitized);

        ClientRenderSettingsComposer composer = new ClientRenderSettingsComposer(sanitized);
        for (Habbo online : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (online != null && online.getClient() != null) online.getClient().sendResponse(composer);
        }
    }

    /** Keeps only the known keys with allowed values; returns null when the payload is not a JSON object. */
    static String sanitize(String raw) {
        JsonObject input;
        try {
            JsonElement element = JsonParser.parseString(raw);
            if (element == null || !element.isJsonObject()) return null;
            input = element.getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }

        JsonObject clean = new JsonObject();

        String resolution = stringOf(input, "resolution");
        if (resolution != null && RESOLUTIONS.contains(resolution) && !"auto".equals(resolution)) clean.addProperty("resolution", resolution);

        String scale = stringOf(input, "scale");
        if (scale != null && SCALES.contains(scale) && !"auto".equals(scale)) clean.addProperty("scale", scale);

        Boolean round = booleanOf(input, "round");
        if (round != null && !round) clean.addProperty("round", false);

        Boolean mask = booleanOf(input, "mask");
        if (mask != null && !mask) clean.addProperty("mask", false);

        Boolean seatAnchor = booleanOf(input, "seatAnchor");
        if (seatAnchor != null && !seatAnchor) clean.addProperty("seatAnchor", false);

        Double maxResolution = numberOf(input, "maxResolution");
        if (maxResolution != null && maxResolution >= 1 && maxResolution <= 4) clean.addProperty("maxResolution", (int) Math.round(maxResolution));

        String pixelated = stringOf(input, "pixelated");
        if ("on".equals(pixelated) || "off".equals(pixelated)) clean.addProperty("pixelated", pixelated);

        Double sitDepth = numberOf(input, "sitDepth");
        if (sitDepth != null && Math.abs(sitDepth) <= 2000 && Math.round(sitDepth) != 0) clean.addProperty("sitDepth", (int) Math.round(sitDepth));

        Double altitudeDepth = numberOf(input, "altitudeDepth");
        if (altitudeDepth != null && altitudeDepth >= 0 && altitudeDepth <= 1 && Math.abs(altitudeDepth - 0.2) > 0.0001) {
            clean.addProperty("altitudeDepth", Math.round(altitudeDepth * 100) / 100.0);
        }

        String canvasSmoothing = stringOf(input, "canvasSmoothing");
        if (canvasSmoothing != null && SMOOTHINGS.contains(canvasSmoothing)) clean.addProperty("canvasSmoothing", canvasSmoothing);

        Boolean antialias = booleanOf(input, "antialias");
        if (antialias != null && antialias) clean.addProperty("antialias", true);

        Boolean backBuffer = booleanOf(input, "backBuffer");
        if (backBuffer != null && !backBuffer) clean.addProperty("backBuffer", false);

        Boolean mipmap = booleanOf(input, "mipmap");
        if (mipmap != null && mipmap) clean.addProperty("mipmap", true);

        String wrap = stringOf(input, "wrap");
        if (wrap != null && WRAPS.contains(wrap) && !"clamp-to-edge".equals(wrap)) clean.addProperty("wrap", wrap);

        Double anisotropy = numberOf(input, "anisotropy");
        if (anisotropy != null && anisotropy >= 1 && anisotropy <= 16) clean.addProperty("anisotropy", (int) Math.round(anisotropy));

        String spriteRound = stringOf(input, "spriteRound");
        if (spriteRound != null && SPRITE_SNAPS.contains(spriteRound) && !"round".equals(spriteRound)) clean.addProperty("spriteRound", spriteRound);

        String offsetRound = stringOf(input, "offsetRound");
        if (offsetRound != null && OFFSET_SNAPS.contains(offsetRound) && !"floor".equals(offsetRound)) clean.addProperty("offsetRound", offsetRound);

        String zoomSnap = stringOf(input, "zoomSnap");
        if (zoomSnap != null && ZOOM_SNAPS.contains(zoomSnap)) clean.addProperty("zoomSnap", zoomSnap);

        // per-furni sit / lay depth: { "<furni class>": <1/1000 tile> }
        JsonElement overridesElement = input.get("sitDepthByClass");
        if (overridesElement != null && overridesElement.isJsonObject()) {
            JsonObject overrides = overridesElement.getAsJsonObject();
            JsonObject cleanOverrides = new JsonObject();
            int count = 0;

            for (String key : overrides.keySet()) {
                if (count >= MAX_SIT_DEPTH_OVERRIDES) break;

                String type = key.trim();
                if (type.isEmpty() || type.length() > 64) continue;

                Double value = numberOf(overrides, key);
                if (value == null || Math.abs(value) > 2000 || Math.round(value) == 0) continue;

                cleanOverrides.addProperty(type, (int) Math.round(value));
                count++;
            }

            if (count > 0) clean.add("sitDepthByClass", cleanOverrides);
        }

        forwardUnknownKeys(input, clean);

        return clean.toString();
    }

    /**
     * Copies keys the emulator does not know about, as long as they are plain primitives. The client is the
     * authority on what a renderer knob means, so a new one only has to ship in the client bundle.
     */
    private static void forwardUnknownKeys(JsonObject input, JsonObject clean) {
        int forwarded = 0;

        for (String key : input.keySet()) {
            if (forwarded >= 16) break;
            if (KNOWN_KEYS.contains(key) || key.length() > 64) continue;

            JsonElement element = input.get(key);
            if (element == null || !element.isJsonPrimitive()) continue;
            if (element.getAsJsonPrimitive().isString() && element.getAsString().length() > 64) continue;

            clean.add(key, element);
            forwarded++;
        }
    }

    private static String stringOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        return element.getAsString().trim().toLowerCase();
    }

    private static Double numberOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            double value = element.getAsJsonPrimitive().isNumber() ? element.getAsDouble() : Double.parseDouble(element.getAsString().trim());
            return Double.isFinite(value) ? value : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Boolean booleanOf(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        if (element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
        String text = element.getAsString().trim().toLowerCase();
        if ("true".equals(text) || "on".equals(text) || "1".equals(text)) return Boolean.TRUE;
        if ("false".equals(text) || "off".equals(text) || "0".equals(text)) return Boolean.FALSE;
        return null;
    }

    private static boolean persist(String json) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO emulator_settings (`key`, `value`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)")) {
            statement.setString(1, CONFIG_KEY);
            statement.setString(2, json);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to persist hotel render settings", e);
            return false;
        }
    }

    private void reply(boolean success, String message) {
        this.client.sendResponse(new CatalogAdminResultComposer(success, PREFIX + message));
    }
}
