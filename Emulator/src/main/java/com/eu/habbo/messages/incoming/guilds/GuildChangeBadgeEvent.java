package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.guilds.GuildChangedBadgeEvent;

public class GuildChangeBadgeEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int guildId = this.packet.readInt();

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);
        if (guild != null) {
            if (guild.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_GUILD_ADMIN)) {
                Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(guild.getRoomId());

                if (room == null || room.getId() != guild.getRoomId())
                    return;

                int count = this.packet.readInt();

                String badge = "";

                byte base = 1;

                while (base < count) {
                    int id = this.packet.readInt();
                    int color = this.packet.readInt();
                    int pos = this.packet.readInt();

                    if (base == 1) {
                        badge += "b";
                    } else {
                        badge += "s";
                    }

                    badge += (id < 100 ? "0" : "") + (id < 10 ? "0" : "") + id + (color < 10 ? "0" : "") + color + "" + pos;

                    base += 3;
                }

                if (guild.getBadge().toLowerCase().equals(badge.toLowerCase()))
                    return;

                GuildChangedBadgeEvent badgeEvent = new GuildChangedBadgeEvent(guild, badge);
                Emulator.getPluginManager().fireEvent(badgeEvent);

                if (badgeEvent.isCancelled())
                    return;

                guild.setBadge(badgeEvent.badge);
                guild.needsUpdate = true;

                if (Emulator.getConfig().getBoolean("imager.internal.enabled")) {
                    Emulator.getBadgeImager().generate(guild);
                }

                room.refreshGuild(guild);
                Emulator.getThreading().run(guild);
            }
        }
    }
}
