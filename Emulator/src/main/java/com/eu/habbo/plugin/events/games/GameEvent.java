package com.eu.habbo.plugin.events.games;

import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.plugin.Event;

public abstract class GameEvent extends Event {

    public final Game game;


    public GameEvent(Game game) {
        this.game = game;
    }
}
