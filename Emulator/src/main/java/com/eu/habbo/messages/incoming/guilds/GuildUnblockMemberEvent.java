package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.GuildRank;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.GuildRefreshMembersListComposer;

/** AIR 13 UnblockGroupMember (2864): an admin lifts a block so the user can join again. */
public class GuildUnblockMemberEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int guildId = this.packet.readInt();
        int userId = this.packet.readInt();

        if (!GuildInputGuard.arePositiveIds(guildId, userId)) {
            return;
        }

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);

        if (guild == null) {
            return;
        }

        GuildMember actor =
                Emulator.getGameEnvironment().getGuildManager().getGuildMember(guild, this.client.getHabbo());
        boolean allowed =
                guild.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()
                        || this.client.getHabbo().hasPermission(Permission.ACC_GUILD_ADMIN)
                        || (actor != null
                                && (actor.getRank().equals(GuildRank.OWNER)
                                        || actor.getRank().equals(GuildRank.ADMIN)));

        if (!allowed) {
            return;
        }

        if (Emulator.getGameEnvironment().getGuildManager().unblockMember(guild, userId)) {
            this.client.sendResponse(new GuildRefreshMembersListComposer(guild));
        }
    }
}
