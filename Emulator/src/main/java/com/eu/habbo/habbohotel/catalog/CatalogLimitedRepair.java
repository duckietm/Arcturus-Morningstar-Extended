package com.eu.habbo.habbohotel.catalog;

import com.eu.habbo.Emulator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconciles {@code catalog_items_limited} with the limited furniture that really exists, every time the LTD pools
 * are (re)loaded.
 *
 * <ul>
 *   <li>Reservations older than ten minutes that never turned into a sale are released.</li>
 *   <li>Every limited furniture in {@code items} marks its number as sold (older builds never did, so a restart
 *       handed the same numbers out again).</li>
 *   <li>Two furniture carrying the same number: the oldest keeps it, the others get the lowest free number of the
 *       edition and their {@code limited_data} is rewritten.</li>
 *   <li>{@code catalog_items.limited_sells} is recomputed for every limited offer.</li>
 * </ul>
 */
final class CatalogLimitedRepair {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogLimitedRepair.class);

    private static final String SOLD_ITEMS_SQL =
            "SELECT ci.id AS catalog_item_id, i.id AS item_id, i.user_id, i.limited_data "
                    + "FROM catalog_items ci "
                    + "JOIN items i ON i.item_id = CAST(SUBSTRING_INDEX(ci.item_ids, ';', 1) AS UNSIGNED) "
                    + "WHERE ci.limited_stack > 0 AND i.limited_data <> '' AND i.limited_data <> '0:0' "
                    + "AND CAST(SUBSTRING_INDEX(i.limited_data, ':', 1) AS UNSIGNED) = ci.limited_stack "
                    + "ORDER BY ci.id, i.id";

    record Result(int released, int marked, int renumbered) {
        boolean changedAnything() {
            return this.released + this.marked + this.renumbered > 0;
        }
    }

    private record Duplicate(int catalogItemId, int itemId, int userId, int number, String stack) {}

    private CatalogLimitedRepair() {}

    static Result run(Map<Integer, CatalogLimitedConfiguration> configurations) {
        int released = 0;
        int marked = 0;
        int renumbered = 0;
        Set<Integer> touched = new HashSet<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            released = releaseStaleReservations(connection);

            Map<Long, Integer> keepers = new HashMap<>();
            List<Duplicate> duplicates = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(SOLD_ITEMS_SQL);
                    ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    int catalogItemId = set.getInt("catalog_item_id");
                    int itemId = set.getInt("item_id");
                    int userId = set.getInt("user_id");
                    String[] data = set.getString("limited_data").split(":");
                    if (data.length != 2) continue;

                    int number;
                    try {
                        number = Integer.parseInt(data[1].trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    if (number <= 0) continue;

                    long key = ((long) catalogItemId << 32) | (number & 0xffffffffL);
                    if (keepers.containsKey(key)) {
                        duplicates.add(new Duplicate(catalogItemId, itemId, userId, number, data[0].trim()));
                        continue;
                    }

                    keepers.put(key, itemId);
                    if (markSold(connection, catalogItemId, number, userId, itemId)) {
                        marked++;
                        touched.add(catalogItemId);
                    }
                }
            }

            for (Duplicate duplicate : duplicates) {
                OptionalInt fresh = claimLowestFree(connection, duplicate);
                if (fresh.isEmpty()) {
                    LOGGER.error(
                            "LTD {}: furniture {} duplicates number {} and the edition has no free number left",
                            duplicate.catalogItemId(), duplicate.itemId(), duplicate.number());
                    continue;
                }

                try (PreparedStatement statement =
                        connection.prepareStatement("UPDATE items SET limited_data = ? WHERE id = ?")) {
                    statement.setString(1, duplicate.stack() + ":" + fresh.getAsInt());
                    statement.setInt(2, duplicate.itemId());
                    statement.executeUpdate();
                }

                LOGGER.warn(
                        "LTD {}: furniture {} (user {}) carried duplicate number {} → renumbered to {}",
                        duplicate.catalogItemId(), duplicate.itemId(), duplicate.userId(), duplicate.number(), fresh.getAsInt());
                renumbered++;
                touched.add(duplicate.catalogItemId());
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE catalog_items ci SET limited_sells = (SELECT COUNT(*) FROM catalog_items_limited l WHERE l.catalog_item_id = ci.id AND l.user_id <> 0) WHERE ci.limited_stack > 0")) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("LTD repair failed", e);
        }

        for (int catalogItemId : touched) {
            CatalogLimitedConfiguration configuration = configurations.get(catalogItemId);
            if (configuration != null) configuration.resync();
        }

        return new Result(released, marked, renumbered);
    }

    private static int releaseStaleReservations(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE catalog_items_limited SET user_id = 0, timestamp = 0 WHERE item_id = 0 AND user_id <> 0 AND timestamp > 0 AND timestamp < ?")) {
            statement.setInt(1, Emulator.getIntUnixTimestamp() - 600);
            return statement.executeUpdate();
        }
    }

    /** Records {@code itemId} as the sale of {@code number}; returns true when the row changed. */
    private static boolean markSold(Connection connection, int catalogItemId, int number, int userId, int itemId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id, item_id FROM catalog_items_limited WHERE catalog_item_id = ? AND number = ? LIMIT 1")) {
            statement.setInt(1, catalogItemId);
            statement.setInt(2, number);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    if (set.getInt("user_id") == userId && set.getInt("item_id") == itemId) {
                        return false;
                    }

                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE catalog_items_limited SET user_id = ?, item_id = ?, timestamp = IF(timestamp = 0, ?, timestamp) WHERE catalog_item_id = ? AND number = ? LIMIT 1")) {
                        update.setInt(1, userId);
                        update.setInt(2, itemId);
                        update.setInt(3, Emulator.getIntUnixTimestamp());
                        update.setInt(4, catalogItemId);
                        update.setInt(5, number);
                        return update.executeUpdate() == 1;
                    }
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT IGNORE INTO catalog_items_limited (catalog_item_id, number, user_id, timestamp, item_id) VALUES (?, ?, ?, ?, ?)")) {
            insert.setInt(1, catalogItemId);
            insert.setInt(2, number);
            insert.setInt(3, userId);
            insert.setInt(4, Emulator.getIntUnixTimestamp());
            insert.setInt(5, itemId);
            return insert.executeUpdate() == 1;
        }
    }

    private static OptionalInt claimLowestFree(Connection connection, Duplicate duplicate) throws SQLException {
        for (int attempt = 0; attempt < 5; attempt++) {
            int candidate;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT number FROM catalog_items_limited WHERE catalog_item_id = ? AND user_id = 0 ORDER BY number LIMIT 1")) {
                statement.setInt(1, duplicate.catalogItemId());
                try (ResultSet set = statement.executeQuery()) {
                    if (!set.next()) return OptionalInt.empty();
                    candidate = set.getInt("number");
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE catalog_items_limited SET user_id = ?, timestamp = ?, item_id = ? WHERE catalog_item_id = ? AND number = ? AND user_id = 0 LIMIT 1")) {
                statement.setInt(1, duplicate.userId());
                statement.setInt(2, Emulator.getIntUnixTimestamp());
                statement.setInt(3, duplicate.itemId());
                statement.setInt(4, duplicate.catalogItemId());
                statement.setInt(5, candidate);
                if (statement.executeUpdate() == 1) return OptionalInt.of(candidate);
            }
        }

        return OptionalInt.empty();
    }
}
