package com.eu.habbo.plugin.events.games;

import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.users.Habbo;

public class GameHabboLeaveEvent extends GameUserEvent {

    public GameHabboLeaveEvent(Game game, Habbo habbo) {
        super(game, habbo);
    }
}
