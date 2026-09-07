package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.GuildRank;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.GuildMembersComposer;

public class RequestGuildMembersEvent extends MessageHandler {
    private static final int MAX_PAGE_ID = 1000;
    private static final int MAX_QUERY_LENGTH = 32;
    private static final int MAX_LEVEL_ID = 2; // 0 all, 1 admins, 2 pending
    private static final int BLOCKED_LEVEL_ID = 3; // the blocked list (AIR 13)

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int groupId = this.packet.readInt();
        int pageId = this.packet.readInt();
        String query = this.packet.readString();
        int levelId = this.packet.readInt();
        boolean pageValid = !(pageId < 0 || pageId > MAX_PAGE_ID);
        boolean levelKnown = levelId >= 0 && (levelId <= MAX_LEVEL_ID || levelId == BLOCKED_LEVEL_ID);
        boolean queryValid = query != null && query.length() <= MAX_QUERY_LENGTH;
        if (!GuildInputGuard.isPositiveId(groupId) || !pageValid || !levelKnown || !queryValid) {
            return;
        }

        Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(groupId);

        if (g != null) {
            boolean isAdmin = this.client.getHabbo().hasPermission(Permission.ACC_GUILD_ADMIN);
            if (!isAdmin && this.client.getHabbo().getHabboStats().hasGuild(g.getId())) {
                GuildMember member =
                        Emulator.getGameEnvironment().getGuildManager().getGuildMember(g, this.client.getHabbo());
                isAdmin = member != null
                        && (member.getRank().equals(GuildRank.OWNER)
                                || member.getRank().equals(GuildRank.ADMIN));
            }

            if (levelId >= 2 && !isAdmin) {
                levelId = 0; // pending and blocked lists are admin-only, like the official dropdown
            }

            this.client.sendResponse(new GuildMembersComposer(
                    g,
                    Emulator.getGameEnvironment().getGuildManager().getGuildMembers(g, pageId, levelId, query),
                    this.client.getHabbo(),
                    pageId,
                    levelId,
                    query,
                    isAdmin,
                    Emulator.getGameEnvironment().getGuildManager().getGuildMembersCount(g, pageId, levelId, query)));
        }
    }
}
