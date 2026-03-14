package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.users.Habbo;

public class GuildFavoriteSetEvent extends GuildEvent {

    public final Habbo habbo;


    public GuildFavoriteSetEvent(Guild guild, Habbo habbo) {
        super(guild);

        this.habbo = habbo;
    }
}
