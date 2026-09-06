package com.eu.habbo.habbohotel.games.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.GamePlayer;
import com.eu.habbo.habbohotel.games.GameState;
import com.eu.habbo.habbohotel.games.GameTeam;
import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.games.freeze.FreezeGame;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameScoreboard;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.users.Habbo;
import java.util.ArrayList;
import java.util.List;

/**
 * Team game driven purely by wired ({@code wf_act_join_team}, {@code wf_act_give_score},
 * {@code wf_act_give_score_tm}).
 *
 * <p>A round is open while {@link #getState()} is not {@link GameState#IDLE}. It opens when a game
 * timer starts ({@link #initialise()}) or {@code wf_act_game_start} runs; rooms without any game
 * timer also open it implicitly on the first team join / score ({@link #ensureRoundOpen()}). It
 * closes through {@link #endRound()} (timer expiry, clock stop/reset, {@code wf_act_game_end}),
 * which records highscore rows via {@link Game#onEnd()} and refreshes the highscore furni. Scores
 * stay readable after the round ends (wired placeholders) and are reset when the next round opens.
 * </p>
 */
public class WiredGame extends Game {
    public static final int RED_EFFECT_ID = 223;
    public static final int BLUE_EFFECT_ID = 224;
    public static final int YELLOW_EFFECT_ID = 225;
    public static final int GREEN_EFFECT_ID = 226;

    private static final GameTeamColors[] SCOREBOARD_COLORS = {
        GameTeamColors.RED, GameTeamColors.GREEN, GameTeamColors.BLUE, GameTeamColors.YELLOW
    };

    public WiredGame(Room room) {
        super(GameTeam.class, GamePlayer.class, room, false);
    }

    /**
     * Timer start / start effect: opens a fresh round (scores reset, blobs re-armed, start time
     * stamped) without recording the previous one; a round is only recorded when it ends.
     */
    @Override
    public void initialise() {
        this.start();
        this.refreshScoreboards();
    }

    @Override
    public void run() {
        // Wired effects drive the scores; there is no per-tick game logic.
    }

    @Override
    public boolean addHabbo(Habbo habbo, GameTeamColors teamColor) {
        this.ensureRoundOpen();
        // The team helmet is optional on this hotel: hotel.wired.team.effect=true restores it.
        if (com.eu.habbo.Emulator.getConfig().getBoolean("hotel.wired.team.effect", false)) {
            this.room.giveEffect(habbo, this.getEffectId(teamColor), -1);
        }
        return super.addHabbo(habbo, teamColor);
    }

    @Override
    public void removeHabbo(Habbo habbo) {
        super.removeHabbo(habbo);
        if (com.eu.habbo.Emulator.getConfig().getBoolean("hotel.wired.team.effect", false)) {
            this.room.giveEffect(habbo, 0, -1);
        }
    }

    /**
     * Ends the open round: {@link Game#onEnd()} writes the {@code items_highscore_data} rows for
     * every team and refreshes the room's highscore furni, then {@link Game#stop()} closes the
     * round. Safe to call repeatedly; only the first call after a round opened does anything.
     *
     * @return {@code true} when a round was open and has now been ended
     */
    private int liveHighscoreSince = com.eu.habbo.Emulator.getIntUnixTimestamp();

    public synchronized boolean endRound() {
        if (this.state == GameState.IDLE) {
            return false;
        }

        this.onEnd();
        this.stop();
        return true;
    }

    /**
     * Opens a round implicitly when a player joins or scores while no round is open. Rooms with a
     * game timer leave the round lifecycle to the timer (start = new round), so pre-round points
     * are discarded there exactly as before instead of being recorded.
     */
    public void ensureRoundOpen() {
        if (this.state != GameState.IDLE || this.room == null) {
            return;
        }

        RoomSpecialTypes types = this.room.getRoomSpecialTypes();
        if (types != null && !types.getGameTimers().isEmpty()) {
            return;
        }

        this.openRound();
    }

    private synchronized void openRound() {
        if (this.state != GameState.IDLE) {
            return;
        }

        this.state = GameState.RUNNING;
        this.startTime = Emulator.getIntUnixTimestamp();
        this.liveHighscoreSince = this.startTime;

        synchronized (this.teams) {
            for (GameTeam team : this.teams.values()) {
                team.resetScores();
            }
        }

        this.refreshScoreboards();
    }

    /**
     * Writes the current team standings to every highscore furni in the room ("punteggio storico"),
     * replacing this round's earlier entries, so the board fills while the game is still running
     * instead of only at {@link Game#onEnd()}.
     */

    /** Every highscore furni in the room, regardless of how RoomSpecialTypes indexed it. */
    private java.util.List<com.eu.habbo.habbohotel.users.HabboItem> highscoreItems() {
        java.util.List<com.eu.habbo.habbohotel.users.HabboItem> items = new java.util.ArrayList<>();
        if (this.room == null) return items;

        for (com.eu.habbo.habbohotel.users.HabboItem item : this.room.getFloorItems()) {
            if (item instanceof com.eu.habbo.habbohotel.items.interactions.InteractionWiredHighscore) items.add(item);
        }
        if (items.isEmpty() && this.room.getRoomSpecialTypes() != null) {
            items.addAll(this.room.getRoomSpecialTypes().getItemsOfType(
                    com.eu.habbo.habbohotel.items.interactions.InteractionWiredHighscore.class));
        }
        return items;
    }

    public void publishLiveHighscores() {
        if (this.room == null) return;

        try {
            com.eu.habbo.habbohotel.wired.highscores.WiredHighscoreManager manager =
                    com.eu.habbo.Emulator.getGameEnvironment().getItemManager().getHighscoreManager();
            if (manager == null) return;

            // one row per player (own score) so the board lists usernames, whatever the team
            java.util.List<com.eu.habbo.habbohotel.games.GamePlayer> players = new java.util.ArrayList<>();
            synchronized (this.teams) {
                for (com.eu.habbo.habbohotel.games.GameTeam team : this.teams.values()) {
                    for (com.eu.habbo.habbohotel.games.GamePlayer member : team.getMembers()) {
                        if (member.getHabbo() != null && member.getScore() > 0) players.add(member);
                    }
                }
            }
            if (players.isEmpty()) return;

            int best = 0;
            for (com.eu.habbo.habbohotel.games.GamePlayer player : players) best = Math.max(best, player.getScore());

            java.util.List<com.eu.habbo.habbohotel.users.HabboItem> boards = this.highscoreItems();
            if (boards.isEmpty()) return;

            int now = Math.max(com.eu.habbo.Emulator.getIntUnixTimestamp(), this.liveHighscoreSince);
            for (com.eu.habbo.habbohotel.users.HabboItem item : boards) {
                java.util.List<com.eu.habbo.habbohotel.wired.highscores.WiredHighscoreDataEntry> fresh = new java.util.ArrayList<>();
                for (com.eu.habbo.habbohotel.games.GamePlayer player : players) {
                    fresh.add(new com.eu.habbo.habbohotel.wired.highscores.WiredHighscoreDataEntry(
                            item.getId(),
                            java.util.Collections.singletonList(player.getHabbo().getHabboInfo().getId()),
                            player.getScore(),
                            player.getScore() >= best,
                            now));
                }

                manager.replaceLiveEntries(item.getId(), this.liveHighscoreSince, fresh);
                item.needsUpdate(true);
                this.room.updateItem(item);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WiredGame.class).error("Live highscore publish failed", e);
        }
    }

    /** Rows are written live, per player; {@link Game#onEnd()} must not add a team-based set on top. */
    @Override
    protected java.util.List<com.eu.habbo.habbohotel.users.HabboItem> highscoreItemsInRoom() {
        return new java.util.ArrayList<>();
    }

    /** Pushes every team total to the matching-colour Freeze/Banzai scoreboards. */
    public void refreshScoreboards() {
        for (GameTeamColors color : SCOREBOARD_COLORS) {
            this.refreshScoreboards(color);
        }
    }

    /**
     * Shows the team's live total on the room's Freeze/Banzai scoreboards of that colour, so a
     * wired-driven game gets a working counter. Skipped while a Freeze/Banzai game is active,
     * because that game owns the boards.
     */
    public void refreshScoreboards(GameTeamColors teamColor) {
        if (this.room == null || teamColor == null || teamColor == GameTeamColors.NONE) {
            return;
        }

        RoomSpecialTypes types = this.room.getRoomSpecialTypes();
        if (types == null || this.otherGameActive()) {
            return;
        }

        GameTeam team = this.getTeam(teamColor);
        String value = Integer.toString(team == null ? 0 : Math.max(0, team.getTotalScore()));

        List<InteractionGameScoreboard> boards = new ArrayList<>();
        boards.addAll(types.getFreezeScoreboards(teamColor).values());
        boards.addAll(types.getBattleBanzaiScoreboards(teamColor).values());

        for (InteractionGameScoreboard board : boards) {
            if (board == null || value.equals(board.getExtradata())) {
                continue;
            }

            board.setExtradata(value);
            this.room.updateItemState(board);
        }
    }

    private boolean otherGameActive() {
        for (Game game : this.room.getGames()) {
            if (game == null || game == this) {
                continue;
            }

            if (game.getState() != GameState.IDLE) {
                return true;
            }
        }

        return false;
    }

    private int getEffectId(GameTeamColors teamColor) {
        switch (teamColor) {
            case RED:
                return RED_EFFECT_ID;
            case BLUE:
                return BLUE_EFFECT_ID;
            case YELLOW:
                return YELLOW_EFFECT_ID;
            case GREEN:
                return GREEN_EFFECT_ID;
            default:
                return FreezeGame.effectId + teamColor.type;
        }
    }
}
