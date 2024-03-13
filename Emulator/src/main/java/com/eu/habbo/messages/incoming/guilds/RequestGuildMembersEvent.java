package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.GuildRank;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.GuildMembersComposer;

public class RequestGuildMembersEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int groupId = this.packet.readInt();
        int pageId = this.packet.readInt();
        String query = this.packet.readString();
        int levelId = this.packet.readInt();

        Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(groupId);

        if (g != null) {
            boolean isAdmin = this.client.getHabbo().hasPermission(Permission.ACC_GUILD_ADMIN);
            if (!isAdmin && this.client.getHabbo().getHabboStats().hasGuild(g.getId())) {
                GuildMember member = Emulator.getGameEnvironment().getGuildManager().getGuildMember(g, this.client.getHabbo());
                isAdmin = member != null && (member.getRank().equals(GuildRank.OWNER) || member.getRank().equals(GuildRank.ADMIN));
            }

            this.client.sendResponse(new GuildMembersComposer(g, Emulator.getGameEnvironment().getGuildManager().getGuildMembers(g, pageId, levelId, query), this.client.getHabbo(), pageId, levelId, query, isAdmin, Emulator.getGameEnvironment().getGuildManager().getGuildMembersCount(g, pageId, levelId, query)));
        }
    }
}
