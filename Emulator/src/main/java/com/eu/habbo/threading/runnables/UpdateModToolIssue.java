package com.eu.habbo.threading.runnables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.modtool.ModToolIssue;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class UpdateModToolIssue implements Runnable {

    private final ModToolIssue issue;

    public UpdateModToolIssue(ModToolIssue issue) {
        this.issue = issue;
    }

    @Override
    public void run() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE support_tickets SET state = ?, type = ?, mod_id = ?, category = ? WHERE id = ?")) {
            statement.setInt(1, this.issue.state.getState());
            statement.setInt(2, this.issue.type.getType());
            statement.setInt(3, this.issue.modId);
            statement.setInt(4, this.issue.category);
            statement.setInt(5, this.issue.id);
            statement.execute();
        } catch (SQLException e) {
            log.error("Caught SQL exception", e);
        }
    }
}
