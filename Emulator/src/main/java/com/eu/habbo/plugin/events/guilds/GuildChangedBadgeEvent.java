package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;

public class GuildChangedBadgeEvent extends GuildEvent {

    public String badge;


    public GuildChangedBadgeEvent(Guild guild, String badge) {
        super(guild);

        this.badge = badge;
    }
}
