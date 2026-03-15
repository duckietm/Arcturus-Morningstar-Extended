package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.users.Habbo;

public class GuildRemovedMemberEvent extends GuildEvent {

    public final int userId;


    public final Habbo guildMember;


    public GuildRemovedMemberEvent(Guild guild, int userId, Habbo guildMember) {
        super(guild);
        this.guildMember = guildMember;
        this.userId = userId;
    }
}
