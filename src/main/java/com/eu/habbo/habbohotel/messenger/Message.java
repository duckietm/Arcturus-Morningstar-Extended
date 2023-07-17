package com.eu.habbo.habbohotel.messenger;

import com.eu.habbo.Emulator;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class Message implements Runnable {
    private final int fromId;
    private final int toId;
    private final int timestamp;
    private String message;

    public Message(int fromId, int toId, String message) {
        this.fromId = fromId;
        this.toId = toId;
        this.message = message;

        this.timestamp = Emulator.getIntUnixTimestamp();
    }

    @Override
    public void run() {
        //TODO Turn into scheduler
        if (Messenger.SAVE_PRIVATE_CHATS) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO chatlogs_private (user_from_id, user_to_id, message, timestamp) VALUES (?, ?, ?, ?)")) {
                statement.setInt(1, this.fromId);
                statement.setInt(2, this.toId);
                statement.setString(3, this.message);
                statement.setInt(4, this.timestamp);
                statement.execute();
            } catch (SQLException e) {
                log.error("Caught SQL exception", e);
            }
        }
    }

    public int getToId() {
        return this.toId;
    }

    public int getFromId() {
        return this.fromId;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTimestamp() {
        return this.timestamp;
    }
}
