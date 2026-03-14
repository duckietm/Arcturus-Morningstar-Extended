package com.eu.habbo.habbohotel.games.wired;

import com.eu.habbo.habbohotel.games.*;
import com.eu.habbo.habbohotel.games.freeze.FreezeGame;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

public class WiredGame extends Game {
    public GameState state = GameState.RUNNING;

    public WiredGame(Room room) {
        super(GameTeam.class, GamePlayer.class, room, false);
    }

    @Override
    public void initialise() {
        this.state = GameState.RUNNING;

        for (GameTeam team : this.teams.values()) {
            team.resetScores();
        }
    }

    @Override
    public void run() {
        this.state = GameState.RUNNING;
    }

    @Override
    public boolean addHabbo(Habbo habbo, GameTeamColors teamColor) {
        this.room.giveEffect(habbo, FreezeGame.effectId + teamColor.type, -1);
        return super.addHabbo(habbo, teamColor);
    }

    @Override
    public void removeHabbo(Habbo habbo) {
        super.removeHabbo(habbo);
        this.room.giveEffect(habbo, 0, -1);
    }

    @Override
    public void stop() {
        this.state = GameState.RUNNING;
    }

    @Override
    public GameState getState() {
        return GameState.RUNNING;
    }
}