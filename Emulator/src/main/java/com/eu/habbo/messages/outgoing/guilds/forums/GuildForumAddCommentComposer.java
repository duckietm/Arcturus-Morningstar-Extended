package com.eu.habbo.messages.outgoing.guilds.forums;

import com.eu.habbo.habbohotel.guilds.forums.ForumThreadComment;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class GuildForumAddCommentComposer extends MessageComposer {
    private final ForumThreadComment comment;

    public GuildForumAddCommentComposer(ForumThreadComment comment) {
        this.comment = comment;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuildForumAddCommentComposer);
        this.response.appendInt(this.comment.getThread().getGuildId()); //guild_id
        this.response.appendInt(this.comment.getThreadId()); //thread_id
        this.comment.serialize(this.response); //Comment
        return this.response;
    }
}