package com.eu.habbo.plugin.events.games;

import com.eu.habbo.habbohotel.games.Game;

public class GameStartedEvent extends GameEvent {

    public GameStartedEvent(Game game) {
        super(game);
    }
}
