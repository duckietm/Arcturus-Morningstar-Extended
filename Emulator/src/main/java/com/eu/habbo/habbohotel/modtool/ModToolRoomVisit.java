package com.eu.habbo.habbohotel.modtool;

import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ModToolRoomVisit implements Comparable<ModToolRoomVisit> {
    public int roomId;
    public String roomName;
    public int timestamp;
    public int exitTimestamp;
    public THashSet<ModToolChatLog> chat;

    public ModToolRoomVisit(ResultSet set) throws SQLException {
        this.roomId = set.getInt("room_id");
        this.roomName = set.getString("name");
        this.timestamp = set.getInt("timestamp");
    }

    public ModToolRoomVisit(int roomId, String roomName, int timestamp, int exitTimestamp) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.timestamp = timestamp;
        this.exitTimestamp = exitTimestamp;
        this.chat = new THashSet<>();
    }

    @Override
    public int compareTo(ModToolRoomVisit o) {
        return o.timestamp - this.timestamp;
    }
}
