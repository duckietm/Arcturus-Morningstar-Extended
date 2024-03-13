package com.eu.habbo.plugin.events.guilds.forums;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.Event;

public class GuildForumThreadBeforeCreated extends Event {
    public final Guild guild;
    public final Habbo opener;
    public final String subject;
    public final String message;

    public GuildForumThreadBeforeCreated(Guild guild, Habbo opener, String subject, String message) {
        this.guild = guild;
        this.opener = opener;
        this.subject = subject;
        this.message = message;
    }
}
