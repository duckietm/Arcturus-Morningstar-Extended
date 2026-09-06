package com.eu.habbo.habbohotel.wired.highscores;

import com.eu.habbo.Emulator;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.util.HotelDateTimeUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiredHighscoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiredHighscoreManager.class);

    private final ConcurrentHashMap<Integer, List<WiredHighscoreDataEntry>> data = new ConcurrentHashMap<>();

    /**
     * Bumped on every in-memory change (load, add, reset). Highscore furni compare it against the
     * revision they rendered last, so a board never serializes a stale row list after a write.
     */
    private final AtomicLong revision = new AtomicLong();

    private static final String locale =
            (System.getProperty("user.language") != null ? System.getProperty("user.language") : "en");
    private static final String country =
            (System.getProperty("user.country") != null ? System.getProperty("user.country") : "US");

    private static final DayOfWeek firstDayOfWeek =
            WeekFields.of(Locale.of(locale, country)).getFirstDayOfWeek();
    private static final DayOfWeek lastDayOfWeek =
            DayOfWeek.of(((firstDayOfWeek.getValue() + 5) % DayOfWeek.values().length) + 1);
    public static ScheduledFuture<?> midnightUpdater = null;

    public void load() {
        long millis = System.currentTimeMillis();

        this.data.clear();
        this.loadHighscoreData();
        this.revision.incrementAndGet();

        LOGGER.info(
                "Highscore Manager -> Loaded! ({} MS, {} items)",
                System.currentTimeMillis() - millis,
                this.data.size());
    }

    @EventHandler
    public static void onEmulatorLoaded(EmulatorLoadedEvent event) {
        if (midnightUpdater != null) {
            midnightUpdater.cancel(true);
        }

        midnightUpdater = Emulator.getThreading()
                .run(new WiredHighscoreMidnightUpdater(), WiredHighscoreMidnightUpdater.getNextUpdaterRun());
    }

    public void dispose() {
        if (midnightUpdater != null) {
            midnightUpdater.cancel(true);
        }

        this.data.clear();
        this.revision.incrementAndGet();
    }

    private void loadHighscoreData() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM items_highscore_data")) {
            statement.setFetchSize(1000);
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    try {
                        WiredHighscoreDataEntry entry = new WiredHighscoreDataEntry(set);

                        this.data
                                .computeIfAbsent(
                                        entry.getItemId(), k -> Collections.synchronizedList(new ArrayList<>()))
                                .add(entry);
                    } catch (RuntimeException e) {
                        // Skip a single malformed row instead of aborting the
                        // entire highscore load.
                        LOGGER.error("Skipping malformed highscore row", e);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    public void addHighscoreData(WiredHighscoreDataEntry entry) {
        this.data
                .computeIfAbsent(entry.getItemId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(entry);
        this.revision.incrementAndGet();

        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO `items_highscore_data` (`item_id`, `user_ids`, `score`, `is_win`, `timestamp`) VALUES (?, ?, ?, ?, ?)")) {
                statement.setInt(1, entry.getItemId());
                statement.setString(2, joinUserIds(entry.getUserIds()));
                statement.setInt(3, entry.getScore());
                statement.setInt(4, entry.isWin() ? 1 : 0);
                statement.setInt(5, entry.getTimestamp());

                statement.execute();
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        });
    }

    private static final Object LIVE_DB_LOCK = new Object();

    /**
     * Replaces the live rows of the current round ({@code timestamp >= since}) of one highscore
     * furni with {@code fresh}, in memory and in {@code items_highscore_data}. The database write
     * runs delete + insert in a single serialized task whose payload is the in-memory state at
     * execution time, so rapid successive publishes always converge on the latest standings.
     */
    public void replaceLiveEntries(int itemId, int since, List<WiredHighscoreDataEntry> fresh) {
        List<WiredHighscoreDataEntry> list =
                this.data.computeIfAbsent(itemId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(entry -> entry.getTimestamp() >= since);
            list.addAll(fresh);
        }
        this.revision.incrementAndGet();

        Emulator.getThreading().run(() -> {
            synchronized (LIVE_DB_LOCK) {
                List<WiredHighscoreDataEntry> snapshot;
                synchronized (list) {
                    snapshot = list.stream()
                            .filter(entry -> entry.getTimestamp() >= since)
                            .collect(Collectors.toList());
                }

                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM `items_highscore_data` WHERE `item_id` = ? AND `timestamp` >= ?")) {
                        delete.setInt(1, itemId);
                        delete.setInt(2, since);
                        delete.execute();
                    }

                    if (snapshot.isEmpty()) return;

                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO `items_highscore_data` (`item_id`, `user_ids`, `score`, `is_win`, `timestamp`) VALUES (?, ?, ?, ?, ?)")) {
                        for (WiredHighscoreDataEntry entry : snapshot) {
                            insert.setInt(1, entry.getItemId());
                            insert.setString(2, entry.getUserIds().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(",")));
                            insert.setInt(3, entry.getScore());
                            insert.setInt(4, entry.isWin() ? 1 : 0);
                            insert.setInt(5, entry.getTimestamp());
                            insert.addBatch();
                        }
                        insert.executeBatch();
                    }
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            }
        });
    }

    public List<WiredHighscoreRow> getHighscoreRowsForItem(
            int itemId, WiredHighscoreClearType clearType, WiredHighscoreScoreType scoreType) {
        if (!this.data.containsKey(itemId)) return null;

        List<WiredHighscoreDataEntry> list = this.data.get(itemId);
        if (list == null) return null;

        List<WiredHighscoreDataEntry> copy;
        synchronized (list) {
            copy = new ArrayList<>(list);
        }

        Stream<WiredHighscoreRow> highscores = copy.stream()
                .filter(entry -> this.timeMatchesEntry(entry, clearType)
                        && (scoreType != WiredHighscoreScoreType.MOSTWIN || entry.isWin()))
                .map(entry -> new WiredHighscoreRow(
                        entry.getUserIds().stream()
                                .map(id -> Emulator.getGameEnvironment()
                                        .getHabboManager()
                                        .getCachedUsername(id))
                                .collect(Collectors.toList()),
                        entry.getScore()));

        if (scoreType == WiredHighscoreScoreType.CLASSIC) {
            return highscores.sorted(WiredHighscoreRow::compareTo).collect(Collectors.toList());
        }

        if (scoreType == WiredHighscoreScoreType.PERTEAM) {
            return highscores.collect(Collectors.groupingBy(h -> h.getUsers().hashCode())).entrySet().stream()
                    .map(e -> e.getValue().stream()
                            .sorted(WiredHighscoreRow::compareTo)
                            .collect(Collectors.toList())
                            .get(0))
                    .sorted(WiredHighscoreRow::compareTo)
                    .collect(Collectors.toList());
        }

        if (scoreType == WiredHighscoreScoreType.MOSTWIN) {
            return highscores.collect(Collectors.groupingBy(h -> h.getUsers().hashCode())).entrySet().stream()
                    .map(e -> new WiredHighscoreRow(
                            e.getValue().get(0).getUsers(), e.getValue().size()))
                    .sorted(WiredHighscoreRow::compareTo)
                    .collect(Collectors.toList());
        }

        if (scoreType == WiredHighscoreScoreType.LONGESTTIME) {
            return bestPerTeam(highscores, true);
        }

        // The fastest time is the same shape read the other way round: keep each team's smallest
        // score and put the smallest first, or the board would crown whoever was slowest.
        if (scoreType == WiredHighscoreScoreType.FASTESTTIME) {
            return bestPerTeam(highscores, false);
        }

        return null;
    }

    private static List<WiredHighscoreRow> bestPerTeam(Stream<WiredHighscoreRow> highscores, boolean longest) {
        Comparator<WiredHighscoreRow> byValue = Comparator.comparingInt(WiredHighscoreRow::getValue);

        return highscores.collect(Collectors.groupingBy(h -> h.getUsers().hashCode())).entrySet().stream()
                .map(e -> (longest
                                ? e.getValue().stream().max(byValue)
                                : e.getValue().stream().min(byValue))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(longest ? byValue.reversed() : byValue)
                .collect(Collectors.toList());
    }

    private boolean timeMatchesEntry(WiredHighscoreDataEntry entry, WiredHighscoreClearType timeType) {
        switch (timeType) {
            case DAILY:
                return entry.getTimestamp() > this.getTodayStartTimestamp()
                        && entry.getTimestamp() < this.getTodayEndTimestamp();
            case WEEKLY:
                return entry.getTimestamp() > this.getWeekStartTimestamp()
                        && entry.getTimestamp() < this.getWeekEndTimestamp();
            case MONTHLY:
                return entry.getTimestamp() > this.getMonthStartTimestamp()
                        && entry.getTimestamp() < this.getMonthEndTimestamp();
            case ALLTIME:
                return true;
        }

        return false;
    }

    public Map<Integer, List<WiredHighscoreDataEntry>> getData() {
        return this.data;
    }

    public List<WiredHighscoreDataEntry> getEntriesForItemId(int itemId) {
        return this.data.get(itemId);
    }

    /**
     * Replace everything this board holds, in memory and on disk.
     *
     * <p>Nothing ever deleted from {@code items_highscore_data}: the table was only read at boot and
     * appended to, so the reset box emptied the board in memory and every score came back at the next
     * start. The write is threaded like the insert, and the memory swap happens first so a board that
     * is read in the same tick already shows the new state.
     */
    public void setEntriesForItemId(int itemId, List<WiredHighscoreDataEntry> entries) {
        List<WiredHighscoreDataEntry> stored = Collections.synchronizedList(new ArrayList<>(entries));
        this.data.put(itemId, stored);
        this.revision.incrementAndGet();

        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                try (PreparedStatement delete =
                        connection.prepareStatement("DELETE FROM `items_highscore_data` WHERE `item_id` = ?")) {
                    delete.setInt(1, itemId);
                    delete.execute();
                }

                if (stored.isEmpty()) {
                    return;
                }

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO `items_highscore_data` (`item_id`, `user_ids`, `score`, `is_win`, `timestamp`) VALUES (?, ?, ?, ?, ?)")) {
                    for (WiredHighscoreDataEntry entry : stored) {
                        insert.setInt(1, entry.getItemId());
                        insert.setString(2, joinUserIds(entry.getUserIds()));
                        insert.setInt(3, entry.getScore());
                        insert.setInt(4, entry.isWin() ? 1 : 0);
                        insert.setInt(5, entry.getTimestamp());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        });
    }

    /** Current in-memory revision; highscore furni reload their rows when it moved since their last render. */
    public long getRevision() {
        return this.revision.get();
    }

    static String joinUserIds(List<Integer> userIds) {
        return String.join(",", userIds.stream().map(Object::toString).collect(Collectors.toList()));
    }

    private long getTodayStartTimestamp() {
        return HotelDateTimeUtil.toEpochSecond(
                HotelDateTimeUtil.localDateTimeNow().with(LocalTime.MIDNIGHT));
    }

    private long getTodayEndTimestamp() {
        return HotelDateTimeUtil.toEpochSecond(HotelDateTimeUtil.localDateTimeNow()
                .with(LocalTime.MIDNIGHT)
                .plusDays(1)
                .plusSeconds(-1));
    }

    private long getWeekStartTimestamp() {
        return HotelDateTimeUtil.toEpochSecond(HotelDateTimeUtil.localDateTimeNow()
                .with(LocalTime.MIDNIGHT)
                .with(TemporalAdjusters.previousOrSame(firstDayOfWeek)));
    }

    private long getWeekEndTimestamp() {
        return HotelDateTimeUtil.toEpochSecond(HotelDateTimeUtil.localDateTimeNow()
                .with(LocalTime.MIDNIGHT)
                .plusDays(1)
                .plusSeconds(-1)
                .with(TemporalAdjusters.nextOrSame(lastDayOfWeek)));
    }

    private long getMonthStartTimestamp() {
        return HotelDateTimeUtil.toEpochSecond(HotelDateTimeUtil.localDateTimeNow()
                .with(LocalTime.MIDNIGHT)
                .with(TemporalAdjusters.firstDayOfMonth()));
    }

    private long getMonthEndTimestamp() {
        return HotelDateTimeUtil.toEpochSecond(HotelDateTimeUtil.localDateTimeNow()
                .with(LocalTime.MIDNIGHT)
                .plusDays(1)
                .plusSeconds(-1)
                .with(TemporalAdjusters.lastDayOfMonth()));
    }
}
