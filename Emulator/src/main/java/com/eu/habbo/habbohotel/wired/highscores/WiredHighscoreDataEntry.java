package com.eu.habbo.habbohotel.wired.highscores;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WiredHighscoreDataEntry {
    private final int itemId;
    private final List<Integer> userIds;
    private int score;
    private final boolean isWin;
    private final int timestamp;

    public WiredHighscoreDataEntry(int itemId, List<Integer> userIds, int score, boolean isWin, int timestamp) {
        this.itemId = itemId;
        this.userIds = userIds;
        this.score = score;
        this.isWin = isWin;
        this.timestamp = timestamp;
    }

    public WiredHighscoreDataEntry(ResultSet set) throws SQLException {
        this.itemId = set.getInt("item_id");
        this.userIds = Arrays.stream(set.getString("user_ids").split(",")).map(Integer::valueOf).collect(Collectors.toList());
        this.score = set.getInt("score");
        this.isWin = set.getInt("is_win") == 1;
        this.timestamp = set.getInt("timestamp");
    }

    public int getItemId() {
        return itemId;
    }

    public List<Integer> getUserIds() {
        return userIds;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) { this.score = score; }

    public boolean isWin() {
        return isWin;
    }

    public int getTimestamp() {
        return timestamp;
    }
}
