package com.eu.habbo.core;

import com.eu.habbo.Emulator;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class DatabaseLogger {

    public void store(final DatabaseLoggable loggable) {
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(loggable.getQuery())) {
                    loggable.log(statement);
                    statement.executeBatch();
                }
            } catch (SQLException e) {
                log.error("Exception caught while saving loggable to database.", e);
            }
        });
    }

}
