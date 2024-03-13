package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
class QueryDeleteHabboBadge implements Runnable {
    private final String name;
    private final Habbo habbo;

    public QueryDeleteHabboBadge(Habbo habbo, String name) {
        this.name = name;
        this.habbo = habbo;
    }

    @Override
    public void run() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM user_badges WHERE users_id = ? AND badge_code = ?")) {
            statement.setInt(1, this.habbo.getHabboInfo().getId());
            statement.setString(2, this.name);
            statement.execute();
        } catch (SQLException e) {
            log.error("Caught SQL exception", e);
        }
    }
}
