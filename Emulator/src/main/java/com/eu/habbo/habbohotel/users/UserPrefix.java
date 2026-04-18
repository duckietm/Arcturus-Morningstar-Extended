package com.eu.habbo.habbohotel.users;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class UserPrefix implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPrefix.class);

    private int id;
    private final int userId;
    private String text;
    private String color;
    private String icon;
    private String effect;
    private boolean active;
    private boolean needsInsert;
    private boolean needsUpdate;
    private boolean needsDelete;

    public UserPrefix(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.userId = set.getInt("user_id");
        this.text = set.getString("text");
        this.color = set.getString("color");
        this.icon = set.getString("icon");
        if (this.icon == null) this.icon = "";
        this.effect = set.getString("effect");
        if (this.effect == null) this.effect = "";
        this.active = set.getBoolean("active");
        this.needsInsert = false;
        this.needsUpdate = false;
        this.needsDelete = false;
    }

    public UserPrefix(int userId, String text, String color, String icon, String effect) {
        this.id = 0;
        this.userId = userId;
        this.text = text;
        this.color = color;
        this.icon = icon != null ? icon : "";
        this.effect = effect != null ? effect : "";
        this.active = false;
        this.needsInsert = true;
        this.needsUpdate = false;
        this.needsDelete = false;
    }

    @Override
    public void run() {
        try {
            if (this.needsInsert) {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO user_prefixes (user_id, text, color, icon, effect, active) VALUES (?, ?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, this.userId);
                    statement.setString(2, this.text);
                    statement.setString(3, this.color);
                    statement.setString(4, this.icon);
                    statement.setString(5, this.effect);
                    statement.setBoolean(6, this.active);
                    statement.execute();
                    try (ResultSet set = statement.getGeneratedKeys()) {
                        if (set.next()) {
                            this.id = set.getInt(1);
                        }
                    }
                }
                this.needsInsert = false;
            } else if (this.needsDelete) {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM user_prefixes WHERE id = ? AND user_id = ?")) {
                    statement.setInt(1, this.id);
                    statement.setInt(2, this.userId);
                    statement.execute();
                }
                this.needsDelete = false;
            } else if (this.needsUpdate) {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                         "UPDATE user_prefixes SET text = ?, color = ?, icon = ?, effect = ?, active = ? WHERE id = ? AND user_id = ?")) {
                    statement.setString(1, this.text);
                    statement.setString(2, this.color);
                    statement.setString(3, this.icon);
                    statement.setString(4, this.effect);
                    statement.setBoolean(5, this.active);
                    statement.setInt(6, this.id);
                    statement.setInt(7, this.userId);
                    statement.execute();
                }
                this.needsUpdate = false;
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public int getId() { return this.id; }
    public int getUserId() { return this.userId; }
    public String getText() { return this.text; }
    public void setText(String text) { this.text = text; }
    public String getColor() { return this.color; }
    public void setColor(String color) { this.color = color; }
    public String getIcon() { return this.icon; }
    public void setIcon(String icon) { this.icon = icon != null ? icon : ""; }
    public String getEffect() { return this.effect; }
    public void setEffect(String effect) { this.effect = effect != null ? effect : ""; }
    public boolean isActive() { return this.active; }

    public void setActive(boolean active) {
        this.active = active;
        this.needsUpdate = true;
    }

    public void needsUpdate(boolean needsUpdate) { this.needsUpdate = needsUpdate; }
    public void needsInsert(boolean needsInsert) { this.needsInsert = needsInsert; }
    public void needsDelete(boolean needsDelete) { this.needsDelete = needsDelete; }
}
