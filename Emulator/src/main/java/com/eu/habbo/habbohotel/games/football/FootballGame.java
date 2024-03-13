package com.eu.habbo.habbohotel.games.football;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.items.interactions.games.football.scoreboards.InteractionFootballScoreboard;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUserAction;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserActionComposer;

import java.util.Map;


public class FootballGame extends Game {
    private Room room;

    public FootballGame(Room room) {
        super(null, null, room, true);

        this.room = room;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void run() {
    }

    public void onScore(RoomUnit kicker, GameTeamColors team) {
        if (this.room == null || !this.room.isLoaded())
            return;

        Habbo habbo = this.room.getHabbo(kicker);
        if (habbo != null) {
            AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("FootballGoalScored"));
            if (habbo.getHabboInfo().getId() != this.room.getOwnerId()) {
                AchievementManager.progressAchievement(this.room.getOwnerId(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("FootballGoalScoredInRoom"));
            }
        }

        this.room.sendComposer(new RoomUserActionComposer(kicker, RoomUserAction.WAVE).compose());

        for (Map.Entry<Integer, InteractionFootballScoreboard> scoreBoard : this.room.getRoomSpecialTypes().getFootballScoreboards(team).entrySet()) {
            scoreBoard.getValue().changeScore(1);
        }
    }
}