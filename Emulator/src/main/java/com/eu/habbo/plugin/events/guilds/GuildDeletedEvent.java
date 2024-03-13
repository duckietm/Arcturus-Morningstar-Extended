package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.users.Habbo;

public class GuildDeletedEvent extends GuildEvent {

    public final Habbo deleter;


    public GuildDeletedEvent(Guild guild, Habbo deleter) {
        super(guild);

        this.deleter = deleter;
    }
}
