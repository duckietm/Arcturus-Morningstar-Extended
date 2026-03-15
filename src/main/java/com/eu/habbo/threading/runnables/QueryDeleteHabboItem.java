package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.HabboItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class QueryDeleteHabboItem implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDeleteHabboItem.class);

    private final int itemId;

    public QueryDeleteHabboItem(int itemId) {
        this.itemId = itemId;
    }

    public QueryDeleteHabboItem(HabboItem item) {
        this.itemId = item.getId();
    }

    @Override
    public void run() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE id = ?")) {
            statement.setInt(1, this.itemId);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }
}
