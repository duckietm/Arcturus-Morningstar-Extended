package com.eu.habbo.core;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLogger.class);

    private final ConcurrentLinkedQueue<DatabaseLoggable> loggables = new ConcurrentLinkedQueue<>();

    public void store(DatabaseLoggable loggable) {
        this.loggables.add(loggable);
    }

    public void save() {
        if (Emulator.getDatabase() == null || Emulator.getDatabase().getDataSource() == null) {
            return;
        }

        if (this.loggables.isEmpty()) {
            return;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            while (!this.loggables.isEmpty()) {
                DatabaseLoggable loggable = this.loggables.remove();

                try (PreparedStatement statement = connection.prepareStatement(loggable.getQuery())) {
                    loggable.log(statement);
                    statement.executeBatch();
                }

            }
        } catch (SQLException e) {
            LOGGER.error("Exception caught while saving loggables to database.", e);
        }
    }

}
