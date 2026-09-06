package com.eu.habbo.habbohotel.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Limited edition (LTD) number pool of one catalog offer.
 *
 * <p>The row in {@code catalog_items_limited} is the source of truth. A number is handed out only after its row has
 * been claimed with an atomic {@code UPDATE ... WHERE user_id = 0}, so a restart, a catalog reload or two clients
 * buying at the same moment can never produce the same number twice. The in-memory list only decides the order in
 * which the unsold numbers are tried (random or sequential, see {@code catalog.ltd.random}).
 *
 * <p>Lifecycle of one number: {@link #reserveNumber(int)} (row reserved for the buyer, {@code item_id = 0}) →
 * {@link #limitedSold(Connection, int, Habbo, HabboItem)} (row completed with the furniture id) or
 * {@link #restoreNumber(int, int)} (reservation released after a failed purchase).
 */
public class CatalogLimitedConfiguration implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogLimitedConfiguration.class);

    /** {@code user_id} written while a purchase is in flight and no buyer is known. */
    public static final int SYSTEM_RESERVATION = -1;

    private final int itemId;
    private final LinkedList<Integer> limitedNumbers;
    private int totalSet;
    // Page the item lived on before a committed sale moved it to the sold-out page,
    // so a later compensated legacy purchase can move it back.
    private int soldOutFromPageId = 0;

    public CatalogLimitedConfiguration(int itemId, LinkedList<Integer> availableNumbers, int totalSet) {
        this(itemId, availableNumbers, totalSet, Emulator.getConfig().getBoolean("catalog.ltd.random", true));
    }

    CatalogLimitedConfiguration(int itemId, LinkedList<Integer> availableNumbers, int totalSet, boolean randomize) {
        this.itemId = itemId;
        this.totalSet = totalSet;
        this.limitedNumbers = availableNumbers;

        if (randomize) {
            Collections.shuffle(this.limitedNumbers);
        } else {
            Collections.reverse(this.limitedNumbers);
        }
    }

    public int getItemId() {
        return this.itemId;
    }

    /**
     * Legacy entry point: reserves a number for an unknown buyer.
     *
     * @throws NoSuchElementException when the edition is sold out
     */
    public int getNumber() {
        return this.reserveNumberOrThrow(SYSTEM_RESERVATION);
    }

    OptionalInt pollNumber() {
        return this.reserveNumber(SYSTEM_RESERVATION);
    }

    /**
     * Reserves the next number for {@code userId}.
     *
     * @throws NoSuchElementException when the edition is sold out
     */
    public int reserveNumberOrThrow(int userId) {
        OptionalInt number = this.reserveNumber(userId);
        if (number.isEmpty()) {
            throw new NoSuchElementException("Limited catalog item " + this.itemId + " is sold out");
        }
        return number.getAsInt();
    }

    /**
     * Reserves the next number for {@code userId}: the row is claimed in the database before the number is
     * returned, so it cannot be handed out twice. Numbers that turn out to be taken in the database (stale pool
     * after a reload, another node, manual edits) are skipped; when the in-memory list runs dry the unsold rows are
     * read once more before giving up.
     */
    public OptionalInt reserveNumber(int userId) {
        int reservedBy = userId == 0 ? SYSTEM_RESERVATION : userId;

        synchronized (this.limitedNumbers) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                for (int attempt = 0; attempt < 2; attempt++) {
                    while (!this.limitedNumbers.isEmpty()) {
                        int number = this.limitedNumbers.pollFirst();
                        if (this.claim(connection, number, reservedBy)) {
                            return OptionalInt.of(number);
                        }
                        LOGGER.warn("LTD {}: number {} is already taken in the database, skipping it", this.itemId, number);
                    }

                    if (attempt == 0) {
                        this.reloadAvailable(connection);
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("LTD {}: unable to reserve a number", this.itemId, e);
            }

            return OptionalInt.empty();
        }
    }

    /** Claims one unsold row; creates the row when the pool was generated in memory only (legacy data). */
    private boolean claim(Connection connection, int number, int reservedBy) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE catalog_items_limited SET user_id = ?, timestamp = ?, item_id = 0 WHERE catalog_item_id = ? AND number = ? AND user_id = 0 LIMIT 1")) {
            statement.setInt(1, reservedBy);
            statement.setInt(2, Emulator.getIntUnixTimestamp());
            statement.setInt(3, this.itemId);
            statement.setInt(4, number);
            if (statement.executeUpdate() == 1) {
                return true;
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id FROM catalog_items_limited WHERE catalog_item_id = ? AND number = ? LIMIT 1")) {
            statement.setInt(1, this.itemId);
            statement.setInt(2, number);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return false;
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO catalog_items_limited (catalog_item_id, number, user_id, timestamp, item_id) VALUES (?, ?, ?, ?, 0)")) {
            statement.setInt(1, this.itemId);
            statement.setInt(2, number);
            statement.setInt(3, reservedBy);
            statement.setInt(4, Emulator.getIntUnixTimestamp());
            return statement.executeUpdate() == 1;
        }
    }

    /** Replaces the in-memory list with the unsold rows of the database. */
    private void reloadAvailable(Connection connection) throws SQLException {
        LinkedList<Integer> fresh = new LinkedList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT number FROM catalog_items_limited WHERE catalog_item_id = ? AND user_id = 0")) {
            statement.setInt(1, this.itemId);
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    fresh.add(set.getInt("number"));
                }
            }
        }

        if (Emulator.getConfig().getBoolean("catalog.ltd.random", true)) {
            Collections.shuffle(fresh);
        } else {
            Collections.sort(fresh);
        }

        this.limitedNumbers.clear();
        this.limitedNumbers.addAll(fresh);
    }

    /** Re-reads the unsold numbers and the size of the edition from the database. */
    void resync() {
        synchronized (this.limitedNumbers) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                this.reloadAvailable(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*) FROM catalog_items_limited WHERE catalog_item_id = ?")) {
                    statement.setInt(1, this.itemId);
                    try (ResultSet set = statement.executeQuery()) {
                        if (set.next() && set.getInt(1) > 0) {
                            this.totalSet = set.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("LTD {}: unable to resync the number pool", this.itemId, e);
            }
        }
    }

    /**
     * Returns a reserved number to the pool when the purchase that reserved it did not complete. Only a
     * reservation ({@code item_id = 0}) is released; a completed sale is never undone. If drawing the number had
     * emptied the pool and moved the item to the sold-out page, the item is moved back.
     */
    public void restoreNumber(int catalogItemId, int number) {
        synchronized (this.limitedNumbers) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "UPDATE catalog_items_limited SET user_id = 0, timestamp = 0 WHERE catalog_item_id = ? AND number = ? AND item_id = 0 LIMIT 1")) {
                statement.setInt(1, catalogItemId);
                statement.setInt(2, number);
                if (statement.executeUpdate() != 1) {
                    // The row is a completed sale (or does not exist): nothing to give back.
                    return;
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception restoring limited number", e);
                return;
            }

            if (!this.limitedNumbers.contains(number)) this.limitedNumbers.push(number);

            if (this.soldOutFromPageId > 0) {
                CatalogItem catalogItem =
                        Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(this.itemId);
                if (catalogItem != null) {
                    Emulator.getGameEnvironment()
                            .getCatalogManager()
                            .moveCatalogItem(catalogItem, this.soldOutFromPageId);
                }
                this.soldOutFromPageId = 0;
            }
        }
    }

    public void restoreNumber(int number) {
        this.restoreNumber(this.itemId, number);
    }

    public void limitedSold(int catalogItemId, Habbo habbo, HabboItem item) {
        synchronized (this.limitedNumbers) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                this.limitedSold(connection, catalogItemId, habbo, item);
                this.markSoldOutIfEmpty();
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        }
    }

    /**
     * Completes the sale of the number carried by {@code item}: the row reserved by {@link #reserveNumber(int)} (or
     * a still unsold row, for legacy callers) gets the furniture id, and {@code catalog_items.limited_sells} is
     * refreshed so the CMS and the catalog show the right count without waiting for a shutdown.
     *
     * @throws SQLException when the number is recorded for another furniture, i.e. it would be a duplicate
     */
    public void limitedSold(Connection connection, int catalogItemId, Habbo habbo, HabboItem item) throws SQLException {
        int userId = habbo.getHabboInfo().getId();
        int number = item.getLimitedSells();

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE catalog_items_limited SET user_id = ?, timestamp = ?, item_id = ? WHERE catalog_item_id = ? AND number = ? AND item_id = 0 AND (user_id = 0 OR user_id = ? OR user_id = ?) LIMIT 1")) {
            statement.setInt(1, userId);
            statement.setInt(2, Emulator.getIntUnixTimestamp());
            statement.setInt(3, item.getId());
            statement.setInt(4, catalogItemId);
            statement.setInt(5, number);
            statement.setInt(6, userId);
            statement.setInt(7, SYSTEM_RESERVATION);
            if (statement.executeUpdate() == 1) {
                this.persistSells(connection, catalogItemId);
                return;
            }
        }

        int existingItemId = -1;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT item_id FROM catalog_items_limited WHERE catalog_item_id = ? AND number = ? LIMIT 1")) {
            statement.setInt(1, catalogItemId);
            statement.setInt(2, number);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) existingItemId = set.getInt("item_id");
            }
        }

        if (existingItemId == item.getId()) {
            return;
        }

        if (existingItemId == -1) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT IGNORE INTO catalog_items_limited (catalog_item_id, number, user_id, timestamp, item_id) VALUES (?, ?, ?, ?, ?)")) {
                statement.setInt(1, catalogItemId);
                statement.setInt(2, number);
                statement.setInt(3, userId);
                statement.setInt(4, Emulator.getIntUnixTimestamp());
                statement.setInt(5, item.getId());
                if (statement.executeUpdate() == 1) {
                    this.persistSells(connection, catalogItemId);
                    return;
                }
            }
        }

        throw new SQLException("Limited catalog number is no longer available: " + number
                + " (catalog item " + catalogItemId + ", furniture " + existingItemId + ")");
    }

    private void persistSells(Connection connection, int catalogItemId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE catalog_items SET limited_sells = (SELECT COUNT(*) FROM catalog_items_limited WHERE catalog_item_id = ? AND user_id <> 0) WHERE id = ?")) {
            statement.setInt(1, catalogItemId);
            statement.setInt(2, catalogItemId);
            statement.executeUpdate();
        }
    }

    public void markSoldOutIfEmpty() {
        synchronized (this.limitedNumbers) {
            if (this.limitedNumbers.isEmpty()) {
                CatalogItem catalogItem =
                        Emulator.getGameEnvironment().getCatalogManager().getCatalogItem(this.itemId);
                if (catalogItem != null) {
                    this.soldOutFromPageId = catalogItem.getPageId();
                    Emulator.getGameEnvironment()
                            .getCatalogManager()
                            .moveCatalogItem(catalogItem, Emulator.getConfig().getInt("catalog.ltd.page.soldout"));
                }
            }
        }
    }

    /** Adds {@code amount} numbers starting at {@code starting}; rows that already exist are kept as they are. */
    public void generateNumbers(int starting, int amount) {
        synchronized (this.limitedNumbers) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT IGNORE INTO catalog_items_limited (catalog_item_id, number) VALUES (?, ?)")) {
                statement.setInt(1, this.itemId);

                int endExclusive = starting + amount;
                for (int i = starting; i < endExclusive; i++) {
                    statement.setInt(2, i);
                    statement.addBatch();
                }

                statement.executeBatch();
                this.reloadAvailable(connection);

                try (PreparedStatement count = connection.prepareStatement(
                        "SELECT COUNT(*) FROM catalog_items_limited WHERE catalog_item_id = ?")) {
                    count.setInt(1, this.itemId);
                    try (ResultSet set = count.executeQuery()) {
                        if (set.next()) this.totalSet = set.getInt(1);
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        }
    }

    public int available() {
        synchronized (this.limitedNumbers) {
            return this.limitedNumbers.size();
        }
    }

    public int getTotalSet() {
        return this.totalSet;
    }

    public void setTotalSet(int totalSet) {
        this.totalSet = totalSet;
    }

    @Override
    public void run() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE catalog_items SET limited_stack = ?, limited_sells = ? WHERE id = ?")) {
            statement.setInt(1, this.totalSet);
            statement.setInt(2, this.totalSet - this.available());
            statement.setInt(3, this.itemId);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }
}
