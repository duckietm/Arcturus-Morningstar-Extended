package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;

public class GuildChangedSettingsEvent extends GuildEvent {

    public int state;

    public boolean rights;

    public GuildChangedSettingsEvent(Guild guild, int state, boolean rights) {
        super(guild);
        this.state = state;
        this.rights = rights;
    }
}
