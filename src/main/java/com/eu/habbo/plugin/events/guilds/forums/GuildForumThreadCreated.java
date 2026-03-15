package com.eu.habbo.plugin.events.guilds.forums;

import com.eu.habbo.habbohotel.guilds.forums.ForumThread;
import com.eu.habbo.plugin.Event;

public class GuildForumThreadCreated extends Event {
    public final ForumThread thread;

    public GuildForumThreadCreated(ForumThread thread) {
        this.thread = thread;
    }
}
