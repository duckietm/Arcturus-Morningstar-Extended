package com.eu.habbo.plugin.events.games;

import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.users.Habbo;

public abstract class GameUserEvent extends GameEvent {

    public final Habbo habbo;


    public GameUserEvent(Game game, Habbo habbo) {
        super(game);

        this.habbo = habbo;
    }
}
