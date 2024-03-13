package com.eu.habbo.messages.outgoing.guilds.forums;

import com.eu.habbo.habbohotel.guilds.forums.ForumThreadComment;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Collection;

public class GuildForumCommentsComposer extends MessageComposer {
    private final int guildId;
    private final int threadId;
    private final int index;
    private final Collection<ForumThreadComment> guildForumCommentList;

    public GuildForumCommentsComposer(int guildId, int threadId, int index, Collection<ForumThreadComment> guildForumCommentList) {
        this.guildId = guildId;
        this.threadId = threadId;
        this.index = index;
        this.guildForumCommentList = guildForumCommentList;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.GuildForumCommentsComposer);

        this.response.appendInt(this.guildId); //guild_id
        this.response.appendInt(this.threadId); //thread_id
        this.response.appendInt(this.index); //start_index
        this.response.appendInt(this.guildForumCommentList.size());

        for (ForumThreadComment comment : this.guildForumCommentList) {
            comment.serialize(this.response);
        }
        return this.response;
    }
}