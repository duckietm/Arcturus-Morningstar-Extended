package com.eu.habbo.plugin.events.guilds.forums;

import com.eu.habbo.habbohotel.guilds.forums.ForumThread;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.Event;

public class GuildForumThreadCommentBeforeCreated extends Event {
    public final ForumThread thread;
    public final Habbo poster;
    public final String message;

    public GuildForumThreadCommentBeforeCreated(ForumThread thread, Habbo poster, String message) {
        this.thread = thread;
        this.poster = poster;
        this.message = message;
    }
}
