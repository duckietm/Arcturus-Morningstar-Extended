package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;

public class GuildChangedNameEvent extends GuildEvent {

    public String name;


    public String description;


    public GuildChangedNameEvent(Guild guild, String name, String description) {
        super(guild);
        this.name = name;
        this.description = description;
    }
}
