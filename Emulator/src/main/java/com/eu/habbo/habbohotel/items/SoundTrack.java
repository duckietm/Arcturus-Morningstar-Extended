package com.eu.habbo.habbohotel.items;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SoundTrack {
    private int id;
    private String name;
    private String author;
    private String code;
    private String data;
    private int length;

    public SoundTrack(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.name = set.getString("name");
        this.author = set.getString("author");
        this.code = set.getString("code");
        this.data = set.getString("track");
        this.length = set.getInt("length");
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getCode() {
        return this.code;
    }

    public String getData() {
        return this.data;
    }

    public int getLength() {
        return this.length;
    }
}
