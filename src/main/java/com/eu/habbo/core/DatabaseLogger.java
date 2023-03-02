package com.eu.habbo.core;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLogger.class);

    public void store(final DatabaseLoggable loggable) {
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(loggable.getQuery())) {
                    loggable.log(statement);
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                LOGGER.error("Exception caught while saving loggable to database.", e);
            }
        });
    }

}
