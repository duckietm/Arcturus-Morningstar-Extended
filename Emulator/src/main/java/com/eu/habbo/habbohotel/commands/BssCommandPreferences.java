package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Persistent, emulator-owned preferences used by the BSS-compatible user commands. */
public final class BssCommandPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(BssCommandPreferences.class);
    private static final Map<Integer, State> CACHE = new ConcurrentHashMap<>();

    public enum Flag {
        DO_NOT_DISTURB("do_not_disturb", false),
        BLOCK_GIFTS("block_gifts", false),
        BLOCK_WHISPERS("block_whispers", false),
        BLOCK_MIMIC("block_mimic", false),
        BLOCK_KISSES("block_kisses", false),
        GROUP_CHAT_ENABLED("group_chat_enabled", true),
        USER_CLICK_ENABLED("user_click_enabled", true),
        RANDOM_WALK_PRIORITY("random_walk_priority", false);

        private final String column;
        private final boolean defaultValue;

        Flag(String column, boolean defaultValue) {
            this.column = column;
            this.defaultValue = defaultValue;
        }
    }

    private BssCommandPreferences() {}

    public static boolean isEnabled(int userId, Flag flag) {
        return state(userId).get(flag);
    }

    /**
     * Non-blocking lookup for hot paths such as pathfinding. A preference becomes cached as soon as
     * its command is used or another guarded action loads it.
     */
    public static boolean isCachedEnabled(int userId, Flag flag) {
        State state = CACHE.get(userId);
        return state == null ? flag.defaultValue : state.get(flag);
    }

    public static boolean toggle(int userId, Flag flag) {
        State state = state(userId);
        boolean enabled;
        synchronized (state) {
            enabled = !state.get(flag);
            state.set(flag, enabled);
        }
        persist(userId, flag, enabled);
        return enabled;
    }

    public static void evict(int userId) {
        CACHE.remove(userId);
    }

    /**
     * Puts {@link Flag#USER_CLICK_ENABLED} back to its default.
     *
     * :tc turns off being clicked, and that is meant to hold for the room it was used in rather than
     * following the user around the hotel, so leaving a room calls this. Only that flag is touched: the
     * other preferences here (do not disturb, block gifts, block whispers) are account-wide on purpose.
     */
    public static void resetRoomScoped(int userId) {
        if (userId <= 0) return;

        Flag flag = Flag.USER_CLICK_ENABLED;
        State state = CACHE.get(userId);

        // Never loaded means never changed, so there is nothing to put back.
        if (state == null) return;

        boolean changed;
        synchronized (state) {
            changed = state.get(flag) != flag.defaultValue;
            if (changed) state.set(flag, flag.defaultValue);
        }

        if (changed) persist(userId, flag, flag.defaultValue);
    }

    private static State state(int userId) {
        if (userId <= 0) return new State();
        return CACHE.computeIfAbsent(userId, BssCommandPreferences::load);
    }

    private static State load(int userId) {
        State state = new State();
        String columns = String.join(", ", java.util.Arrays.stream(Flag.values())
                .map(flag -> "`" + flag.column + "`")
                .toList());

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT " + columns + " FROM bss_user_preferences WHERE user_id = ? LIMIT 1")) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    for (Flag flag : Flag.values()) {
                        state.set(flag, result.getBoolean(flag.column));
                    }
                }
            }
        } catch (SQLException exception) {
            LOGGER.error("Failed to load BSS command preferences for user {}", userId, exception);
        }
        return state;
    }

    private static void persist(int userId, Flag flag, boolean enabled) {
        String sql = "INSERT INTO bss_user_preferences (user_id, `" + flag.column
                + "`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `" + flag.column + "` = VALUES(`"
                + flag.column + "`)";
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setBoolean(2, enabled);
            statement.executeUpdate();
        } catch (SQLException exception) {
            LOGGER.error(
                    "Failed to persist BSS preference {} for user {}", flag.column, userId, exception);
        }
    }

    private static final class State {
        private final EnumMap<Flag, Boolean> values = new EnumMap<>(Flag.class);

        private State() {
            for (Flag flag : Flag.values()) {
                values.put(flag, flag.defaultValue);
            }
        }

        private boolean get(Flag flag) {
            return values.getOrDefault(flag, flag.defaultValue);
        }

        private void set(Flag flag, boolean enabled) {
            values.put(flag, enabled);
        }
    }
}
