package com.eu.habbo.messages.incoming.guilds.forums;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.GuildRank;
import com.eu.habbo.habbohotel.guilds.forums.ForumThread;
import com.eu.habbo.habbohotel.guilds.forums.ForumThreadState;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumThreadMessagesComposer;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumThreadsComposer;
import com.eu.habbo.messages.outgoing.handshake.ConnectionErrorComposer;


public class GuildForumModerateThreadEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int guildId = packet.readInt();
        int threadId = packet.readInt();
        int state = packet.readInt();
        // STATE 20 - HIDDEN_BY_GUILD_ADMIN = HIDDEN BY GUILD ADMINS/ HOTEL MODERATORS
        // STATE 1 = VISIBLE THREAD

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);
        ForumThread thread = ForumThread.getById(threadId);

        if (guild == null || thread == null) {
            this.client.sendResponse(new ConnectionErrorComposer(404));
            return;
        }

        if (thread.getGuildId() != guildId) {
            this.client.sendResponse(new ConnectionErrorComposer(403));
            return;
        }

        GuildMember member = Emulator.getGameEnvironment().getGuildManager().getGuildMember(guildId, this.client.getHabbo().getHabboInfo().getId());
        boolean hasStaffPerms = this.client.getHabbo().hasPermission(Permission.ACC_MODTOOL_TICKET_Q);

        if (member == null && !hasStaffPerms && guild.getOwnerId() != this.client.getHabbo().getHabboInfo().getId()) {
            this.client.sendResponse(new ConnectionErrorComposer(401));
            return;
        }

        boolean isGuildAdmin = (guild.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || (member != null && member.getRank().equals(GuildRank.ADMIN)));

        if (!isGuildAdmin && !hasStaffPerms) {
            this.client.sendResponse(new ConnectionErrorComposer(403));
            return;
        }

        // Restrict state 20 (staff hidden) to staff only
        if (state == 20 && !hasStaffPerms) {
            this.client.sendResponse(new ConnectionErrorComposer(403));
            return;
        }

        thread.setState(ForumThreadState.fromValue(state)); // sets state as defined in the packet
        thread.run();

        switch (state) {
            case 10:
            case 20:
                this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FORUMS_THREAD_HIDDEN.key).compose());
                break;
            case 1:
                this.client.sendResponse(new BubbleAlertComposer(BubbleAlertKeys.FORUMS_THREAD_RESTORED.key).compose());
                break;
        }

        this.client.sendResponse(new GuildForumThreadMessagesComposer(thread));
        this.client.sendResponse(new GuildForumThreadsComposer(guild, 0));
    }
}