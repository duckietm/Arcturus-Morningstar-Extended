package com.eu.habbo.plugin.events.bots;

import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;

public class BotPlacedEvent extends BotEvent {

    public final RoomTile location;


    public final Habbo placer;

    public BotPlacedEvent(Bot bot, RoomTile location, Habbo placer) {
        super(bot);

        this.location = location;
        this.placer = placer;
    }
}
