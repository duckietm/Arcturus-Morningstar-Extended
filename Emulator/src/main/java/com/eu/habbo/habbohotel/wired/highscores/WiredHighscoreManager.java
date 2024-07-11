package com.eu.habbo.habbohotel.wired.highscores;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WiredHighscoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WiredHighscoreManager.class);

    private final static String locale = (System.getProperty("user.language") != null ? System.getProperty("user.language") : "en");
    private final static String country = (System.getProperty("user.country") != null ? System.getProperty("user.country") : "US");

    private final static DayOfWeek firstDayOfWeek = WeekFields.of(new Locale(locale, country)).getFirstDayOfWeek();
    private final static DayOfWeek lastDayOfWeek = DayOfWeek.of(((firstDayOfWeek.getValue() + 5) % DayOfWeek.values().length) + 1);
    private final static ZoneId zoneId = ZoneId.systemDefault();

    public void load() {
        LOGGER.info("Highscore Manager -> Loaded");
    }

    public void addHighscoreData(WiredHighscoreDataEntry entry) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO `items_highscore_data` (`item_id`, `user_ids`, `score`, `is_win`, `timestamp`) VALUES (?, ?, ?, ?, ?)")) {
            statement.setInt(1, entry.getItemId());
            statement.setString(2, String.join(",", entry.getUserIds().stream().map(Object::toString).collect(Collectors.toList())));
            statement.setInt(3, entry.getScore());
            statement.setInt(4, entry.isWin() ? 1 : 0);
            statement.setInt(5, entry.getTimestamp());

            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public void addOrUpdateHighscoreData(WiredHighscoreDataEntry entry) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            final String userIds = String.join(",", entry.getUserIds().stream().map(Object::toString).collect(Collectors.toList()));

            // Select existing
            try (PreparedStatement selectScore = connection.prepareStatement("SELECT `id` FROM `items_highscore_data` WHERE `item_id` = ? AND `user_ids` = ? LIMIT 1")) {
                selectScore.setInt(1, entry.getItemId());
                selectScore.setString(2, userIds);

                try (final ResultSet scoreResult = selectScore.executeQuery()) {
                    if (scoreResult.next()) {
                        // Update
                        try (PreparedStatement statement = connection.prepareStatement("UPDATE `items_highscore_data` SET `score` = `score` + ? WHERE `id` = ?")) {
                            statement.setInt(1, entry.getScore());
                            statement.setInt(2, scoreResult.getInt("id"));
                            statement.execute();
                        }
                    } else {
                        // Insert
                        addHighscoreData(entry);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public void setHighscoreData(WiredHighscoreDataEntry entry) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            final String userIds = String.join(",", entry.getUserIds().stream().map(Object::toString).collect(Collectors.toList()));

            // Select existing
            try (PreparedStatement selectScore = connection.prepareStatement("SELECT `id` FROM `items_highscore_data` WHERE `item_id` = ? AND `user_ids` = ? LIMIT 1")) {
                selectScore.setInt(1, entry.getItemId());
                selectScore.setString(2, userIds);

                try (final ResultSet scoreResult = selectScore.executeQuery()) {
                    if (scoreResult.next()) {
                        // Set
                        try (PreparedStatement statement = connection.prepareStatement("UPDATE `items_highscore_data` SET `score` = ? WHERE `id` = ?")) {
                            statement.setInt(1, entry.getScore());
                            statement.setInt(2, scoreResult.getInt("id"));
                            statement.execute();
                        }
                    } else {
                        // Insert
                        addHighscoreData(entry);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private List<WiredHighscoreDataEntry> loadHighscoreData(final int itemId) {
        final List<WiredHighscoreDataEntry> result = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM items_highscore_data WHERE `item_id` = ?")) {
            statement.setInt(1, itemId);

            try (final ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    result.add(new WiredHighscoreDataEntry(set));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return result;
    }

    public WiredHighscoreDataEntry getHighscoreRow(final int itemId, final List<Integer> userIds) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            final String userIdsStr = String.join(",", userIds.stream().map(Object::toString).collect(Collectors.toList()));

            // Select existing
            try (PreparedStatement selectScore = connection.prepareStatement("SELECT * FROM `items_highscore_data` WHERE `item_id` = ? AND `user_ids` = ? LIMIT 1")) {
                selectScore.setInt(1, itemId);
                selectScore.setString(2, userIdsStr);

                try (final ResultSet scoreResult = selectScore.executeQuery()) {
                    if (scoreResult.next()) {
                        return new WiredHighscoreDataEntry(scoreResult);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return null;
    }

    public List<WiredHighscoreRow> getHighscoreRowsForItem(int itemId, WiredHighscoreClearType clearType, WiredHighscoreScoreType scoreType) {
        final List<WiredHighscoreDataEntry> highscoreData = this.loadHighscoreData(itemId);

        if (highscoreData.size() == 0) {
            return null;
        }

        Stream<WiredHighscoreRow> highscores = new ArrayList<>(highscoreData).stream()
                .filter(entry -> this.timeMatchesEntry(entry, clearType) && (scoreType != WiredHighscoreScoreType.MOSTWIN || entry.isWin()))
                .map(entry -> new WiredHighscoreRow(
                        entry.getUserIds().stream()
                                .map(id -> Emulator.getGameEnvironment().getHabboManager().getHabboInfo(id).getUsername())
                                .collect(Collectors.toList()),
                        entry.getScore()
                ));

        if (scoreType == WiredHighscoreScoreType.CLASSIC) {
            return highscores.sorted(WiredHighscoreRow::compareTo).collect(Collectors.toList());
        }

        if (scoreType == WiredHighscoreScoreType.PERTEAM) {
            return highscores
                    .collect(Collectors.groupingBy(h -> h.getUsers().hashCode()))
                    .entrySet()
                    .stream()
                    .map(e -> e.getValue().stream()
                            .sorted(WiredHighscoreRow::compareTo)
                            .collect(Collectors.toList())
                            .get(0)
                    )
                    .sorted(WiredHighscoreRow::compareTo)
                    .collect(Collectors.toList());
        }

        if (scoreType == WiredHighscoreScoreType.MOSTWIN) {
            return highscores
                    .collect(Collectors.groupingBy(h -> h.getUsers().hashCode()))
                    .entrySet()
                    .stream()
                    .map(e -> new WiredHighscoreRow(e.getValue().get(0).getUsers(), e.getValue().size()))
                    .sorted(WiredHighscoreRow::compareTo)
                    .collect(Collectors.toList());
        }

        return null;
    }

    private boolean timeMatchesEntry(WiredHighscoreDataEntry entry, WiredHighscoreClearType timeType) {
        switch (timeType) {
            case DAILY:
                return entry.getTimestamp() > this.getTodayStartTimestamp() && entry.getTimestamp() < this.getTodayEndTimestamp();
            case WEEKLY:
                return entry.getTimestamp() > this.getWeekStartTimestamp() && entry.getTimestamp() < this.getWeekEndTimestamp();
            case MONTHLY:
                return entry.getTimestamp() > this.getMonthStartTimestamp() && entry.getTimestamp() < this.getMonthEndTimestamp();
            case ALLTIME:
                return true;
        }

        return false;
    }

    private long getTodayStartTimestamp() {
        return LocalDateTime.now().with(LocalTime.MIDNIGHT).atZone(zoneId).toEpochSecond();
    }

    private long getTodayEndTimestamp() {
        return LocalDateTime.now().with(LocalTime.MIDNIGHT).plusDays(1).plusSeconds(-1).atZone(zoneId).toEpochSecond();
    }

    private long getWeekStartTimestamp() {
        return LocalDateTime.now().with(LocalTime.MIDNIGHT).with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).atZone(zoneId).toEpochSecond();
    }

    private long getWeekEndTimestamp() {
        return LocalDateTime.now().with(LocalTime.MIDNIGHT).plusDays(1).plusSeconds(-1).with(TemporalAdjusters.nextOrSame(lastDayOfWeek)).atZone(zoneId).toEpochSecond();
    }

    private long getMonthStartTimestamp() {
        return LocalDateTime.now().with(LocalTime.MIDNIGHT).with(TemporalAdjusters.firstDayOfMonth()).atZone(zoneId).toEpochSecond();
    }

    private long getMonthEndTimestamp() {
        return LocalDateTime.now().with(LocalTime.MIDNIGHT).plusDays(1).plusSeconds(-1).with(TemporalAdjusters.lastDayOfMonth()).atZone(zoneId).toEpochSecond();
    }
}
