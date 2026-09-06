package com.eu.habbo.habbohotel.games.battlebanzai;

import com.eu.habbo.Emulator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Battle Banzai teleport timings, tunable while the hotel is running.
 *
 * <p>These are hotel-wide, not per room: one set of numbers for every banzai in every room. They were only
 * reachable through SQL plus an RCON reload, which makes finding a feel that plays well a slow guessing
 * game - this exists so the values can be nudged and felt immediately.
 *
 * <p>Applying is instant: every one of these is read from {@link com.eu.habbo.core.ConfigurationManager} at
 * the moment it is used rather than cached. BattleBanzaiGame used to hold the flood-fill cooldown in a
 * {@code static final} read once at class load, which made it the one value needing a restart; it reads
 * live now like the rest.
 */
public final class BanzaiSpeedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BanzaiSpeedService.class);

    /** Setting key -> default, in the order the panel shows them. */
    public static final Map<String, Integer> KEYS = new LinkedHashMap<>();

    static {
        // How long the unit stands on the pad before it is picked up.
        KEYS.put("hotel.banzai.teleport.start_delay_ms", 350);
        // The hop itself.
        KEYS.put("hotel.banzai.teleport.jump_ms", 70);
        // How long the unit is held at the destination before it can move again.
        KEYS.put("hotel.banzai.teleport.release_ms", 220);
        // How long the pad it left stays busy.
        KEYS.put("hotel.banzai.teleport.source_reset_ms", 160);
        // Minimum gap between two flood fills of the same game - how fast tiles can be claimed.
        KEYS.put("hotel.banzai.fill.cooldown_ms", 60);
    }

    /** Nothing instant is worth a value outside this: below 20 ms the client cannot animate it. */
    public static final int MINIMUM_MS = 20;

    public static final int MAXIMUM_MS = 5000;

    private BanzaiSpeedService() {}

    /** The values in {@link #KEYS} order, as they are right now. */
    public static int[] current() {
        int[] values = new int[KEYS.size()];
        int index = 0;

        for (Map.Entry<String, Integer> entry : KEYS.entrySet()) {
            values[index++] = Emulator.getConfig().getInt(entry.getKey(), entry.getValue());
        }

        return values;
    }

    /**
     * Persists the given values and makes them live.
     *
     * <p>Every value is clamped rather than rejected: a panel that silently refuses input is worse than one
     * that shows what it settled on, and the caller is sent the resulting values either way.
     *
     * @return true when the settings were written; false leaves the current values untouched.
     */
    public static boolean apply(int[] values) {
        if (values == null || values.length != KEYS.size()) return false;

        int index = 0;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO emulator_settings (`key`, `value`) VALUES (?, ?) "
                                + "ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)")) {

            for (String key : KEYS.keySet()) {
                statement.setString(1, key);
                statement.setString(2, Integer.toString(clamp(values[index++])));
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException exception) {
            LOGGER.error("Could not save the banzai speeds", exception);
            return false;
        }

        // Same path the "updateconfig" RCON key uses: re-read emulator_settings into the live configuration.
        Emulator.getConfig().reload();
        return true;
    }

    public static int clamp(int value) {
        return Math.max(MINIMUM_MS, Math.min(MAXIMUM_MS, value));
    }
}
