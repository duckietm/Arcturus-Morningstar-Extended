package com.eu.habbo.messages.incoming.guilds.forums;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.SettingsState;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumDataComposer;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumThreadsComposer;
import com.eu.habbo.messages.outgoing.handshake.ConnectionErrorComposer;

public class GuildForumThreadsEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int guildId = packet.readInt();
        int index = packet.readInt();

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);

        if (guild == null || !guild.hasForum()) {
            this.client.sendResponse(new ConnectionErrorComposer(404));
            return;
        }

        // Enforce read permissions
        boolean isStaff = this.client.getHabbo().hasPermission(Permission.ACC_MODTOOL_TICKET_Q);
        if (!isStaff && guild.canReadForum() != SettingsState.EVERYONE) {
            GuildMember member = Emulator.getGameEnvironment().getGuildManager().getGuildMember(guildId, this.client.getHabbo().getHabboInfo().getId());
            if (guild.canReadForum() == SettingsState.MEMBERS && member == null) {
                this.client.sendResponse(new ConnectionErrorComposer(403));
                return;
            }
            if (guild.canReadForum() == SettingsState.ADMINS && (member == null || member.getRank().type >= com.eu.habbo.habbohotel.guilds.GuildRank.MEMBER.type)) {
                this.client.sendResponse(new ConnectionErrorComposer(403));
                return;
            }
        }

        this.client.sendResponse(new GuildForumDataComposer(guild, this.client.getHabbo()));
        this.client.sendResponse(new GuildForumThreadsComposer(guild, index));
    }
}