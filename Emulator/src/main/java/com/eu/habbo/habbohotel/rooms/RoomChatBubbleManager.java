package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomChatBubbleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomChatBubbleManager.class);
    private final Map<Integer, BubbleAvailability> availability = new ConcurrentHashMap<>();

    public RoomChatBubbleManager() {
        this.reload();
    }

    public void reload() {
        RoomChatMessageBubbles.removeDynamicBubbles();
        this.availability.clear();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM chat_bubbles");
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int type = resultSet.getInt("type");
                String name = resultSet.getString("name");
                String permission = resultSet.getString("permission");
                boolean overridable = resultSet.getBoolean("overridable");
                boolean triggersTalkingFurniture = resultSet.getBoolean("triggers_talking_furniture");
                boolean enabled = resultSet.getBoolean("enabled");
                int minRank = resultSet.getInt("min_rank");
                int maxRank = readInt(resultSet, "max_rank");
                Timestamp availableFromValue = resultSet.getTimestamp("available_from");
                Instant availableFrom = availableFromValue == null ? null : availableFromValue.toInstant();
                int durationWeeks = resultSet.getInt("duration_weeks");

                RoomChatMessageBubbles.addDynamicBubble(
                        type, name, permission, overridable, triggersTalkingFurniture);
                this.availability.put(
                        type, new BubbleAvailability(enabled, minRank, maxRank, availableFrom, durationWeeks));
            }
        } catch (SQLException exception) {
            LOGGER.error("Failed to load chat bubbles from database.", exception);
        }
    }

    public boolean canUse(int type, Habbo habbo) {
        if (habbo == null) return false;

        RoomChatMessageBubbles bubble = RoomChatMessageBubbles.getBubble(type);
        if (!bubble.getPermission().isBlank() && !habbo.hasPermission(bubble.getPermission())) return false;

        BubbleAvailability rule = this.availability.get(type);
        if (rule == null) return type >= 0 && type <= 53;
        int rank = habbo.getHabboInfo().getRank().getId();
        if (!rule.enabled() || rank < rule.minRank()) return false;
        // CUSTOM: max_rank hides a bubble from higher ranks (e.g. a users-only bubble the staff must not use).
        if (rule.maxRank() > 0 && rank > rule.maxRank()) return false;
        if (rule.availableFrom() == null) return true;

        Instant now = Instant.now();
        if (now.isBefore(rule.availableFrom())) return false;
        return rule.durationWeeks() <= 0
                || now.isBefore(rule.availableFrom().plus(rule.durationWeeks(), ChronoUnit.WEEKS));
    }

    /** Column added by a later migration: tolerate an older schema. */
    private static int readInt(ResultSet resultSet, String column) {
        try {
            return resultSet.getInt(column);
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private record BubbleAvailability(boolean enabled, int minRank, int maxRank, Instant availableFrom, int durationWeeks) {}
}
