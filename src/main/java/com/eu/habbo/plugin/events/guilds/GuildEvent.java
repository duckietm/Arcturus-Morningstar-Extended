package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.plugin.Event;

public abstract class GuildEvent extends Event {

    public final Guild guild;


    public GuildEvent(Guild guild) {
        this.guild = guild;
    }
}
