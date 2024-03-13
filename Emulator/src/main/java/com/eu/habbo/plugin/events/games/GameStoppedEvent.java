package com.eu.habbo.plugin.events.games;

import com.eu.habbo.habbohotel.games.Game;

public class GameStoppedEvent extends GameEvent {

    public GameStoppedEvent(Game game) {
        super(game);
    }
}
