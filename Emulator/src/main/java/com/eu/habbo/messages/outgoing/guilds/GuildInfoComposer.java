package com.eu.habbo.messages.outgoing.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.guilds.GuildMembershipStatus;
import com.eu.habbo.habbohotel.guilds.GuildRank;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GuildInfoComposer extends MessageComposer {
    private final Guild guild;
    private final GameClient client;
    private final boolean newWindow;
    private final GuildMember member;

    public GuildInfoComposer(Guild guild, GameClient client, boolean newWindow, GuildMember member) {
        this.guild = guild;
        this.client = client;
        this.newWindow = newWindow;
        this.member = member;
    }

    @Override
    protected ServerMessage composeInternal() {
            boolean adminPermissions = this.client.getHabbo().getHabboStats().hasGuild(this.guild.getId()) && this.client.getHabbo().hasPermission(Permission.ACC_GUILD_ADMIN) || Emulator.getGameEnvironment().getGuildManager().getOnlyAdmins(guild).get(this.client.getHabbo().getHabboInfo().getId()) != null;
            this.response.init(Outgoing.GuildInfoComposer);
            this.response.appendInt(this.guild.getId());
            this.response.appendBoolean(true);
            this.response.appendInt(this.guild.getState().state);
            this.response.appendString(this.guild.getName());
            this.response.appendString(this.guild.getDescription());
            this.response.appendString(this.guild.getBadge());
            this.response.appendInt(this.guild.getRoomId());
            this.response.appendString(this.guild.getRoomName());
            this.response.appendInt((this.member == null ? GuildMembershipStatus.NOT_MEMBER : this.member.getMembershipStatus()).getStatus());
            this.response.appendInt(this.guild.getMemberCount());
            this.response.appendBoolean(this.client.getHabbo().getHabboStats().guild == this.guild.getId()); // favorite group
            this.response.appendString(new SimpleDateFormat("dd-MM-yyyy").format(new Date(this.guild.getDateCreated() * 1000L)));
            this.response.appendBoolean(adminPermissions || (this.guild.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()));
            this.response.appendBoolean(adminPermissions || (this.member != null && (this.member.getRank().equals(GuildRank.ADMIN))));

            this.response.appendString(this.guild.getOwnerName());
            this.response.appendBoolean(this.newWindow);
            this.response.appendBoolean(this.guild.getRights());
            this.response.appendInt((adminPermissions || this.guild.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()) ? this.guild.getRequestCount() : 0); //Guild invites count.
            this.response.appendBoolean(this.guild.hasForum());
            return this.response;
        }
    }
