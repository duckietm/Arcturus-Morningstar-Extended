package com.eu.habbo.messages.outgoing.guilds.forums;

import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.forums.ForumThread;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class ThreadUpdatedMessageComposer extends MessageComposer {

    public final Guild guild;

    public final ForumThread thread;

    private final Habbo habbo;

    private final boolean isPinned;

    private final boolean isLocked;

    public ThreadUpdatedMessageComposer(Guild guild, ForumThread thread, Habbo habbo, boolean isPinned, boolean isLocked) {
        this.guild = guild;
        this.habbo = habbo;
        this.thread = thread;
        this.isPinned = isPinned;
        this.isLocked = isLocked;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.ThreadUpdateMessageComposer);
        this.response.appendInt(this.thread.getGuildId());
        this.thread.serialize(this.response);

        return this.response;
    }
}