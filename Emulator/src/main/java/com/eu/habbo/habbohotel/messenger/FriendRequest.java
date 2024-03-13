package com.eu.habbo.habbohotel.messenger;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FriendRequest {
    private int id;
    private String username;
    private String look;

    public FriendRequest(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.username = set.getString("username");
        this.look = set.getString("look");
    }

    public FriendRequest(int id, String username, String look) {
        this.id = id;
        this.username = username;
        this.look = look;
    }

    public int getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getLook() {
        return this.look;
    }
}
