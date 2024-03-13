package com.eu.habbo.plugin.events.guilds;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.users.Habbo;

public class GuildAcceptedMembershipEvent extends GuildEvent {

    public final int userId;


    public final Habbo user;


    public GuildAcceptedMembershipEvent(Guild guild, int userId, Habbo user) {
        super(guild);

        this.userId = userId;
        this.user = user;
    }
}
