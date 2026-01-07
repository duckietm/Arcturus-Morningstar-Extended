package com.eu.habbo.habbohotel.games;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredManager;

public class GamePlayer {

    private final Habbo habbo;


    private GameTeamColors teamColor;


    private int score;
    private int wiredScore;


    public GamePlayer(Habbo habbo, GameTeamColors teamColor) {
        this.habbo = habbo;
        this.teamColor = teamColor;
    }


    public void reset() {
        this.score = 0;
        this.wiredScore = 0;
    }

    public synchronized void addScore(int amount) {
        addScore(amount, false);
    }

    public synchronized void addScore(int amount, boolean isWired) {
        if (habbo.getHabboInfo().getGamePlayer() != null && this.habbo.getHabboInfo().getCurrentGame() != null && this.habbo.getHabboInfo().getCurrentRoom().getGame(this.habbo.getHabboInfo().getCurrentGame()).getTeamForHabbo(this.habbo) != null) {
            this.score += amount;

            if (this.score < 0) this.score = 0;

            if(isWired && this.score > 0) {
                this.wiredScore += amount;
            }

            WiredManager.triggerScoreAchieved(this.habbo.getHabboInfo().getCurrentRoom(), this.habbo.getRoomUnit(), this.score);
        }
    }

    public Habbo getHabbo() {
        return this.habbo;
    }


    public GameTeamColors getTeamColor() {
        return this.teamColor;
    }


    public int getScore() {
        return this.score;
    }

    public int getScoreAchievementValue() {
        return this.score - this.wiredScore;
    }
}
