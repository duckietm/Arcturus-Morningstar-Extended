package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildState;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.habbohotel.guilds.forums.ForumThread;
import com.eu.habbo.messages.incoming.guilds.forums.GuildForumListEvent;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumDataComposer;
import com.eu.habbo.plugin.events.guilds.GuildChangedSettingsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GuildChangeSettingsEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildChangeSettingsEvent.class);

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int guildId = this.packet.readInt();

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);

        if (guild != null) {
            if (guild.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_GUILD_ADMIN)) {
                GuildChangedSettingsEvent settingsEvent = new GuildChangedSettingsEvent(guild, this.packet.readInt(), this.packet.readInt() == 0);
                Emulator.getPluginManager().fireEvent(settingsEvent);

                if (settingsEvent.isCancelled())
                    return;

                guild.setState(GuildState.valueOf(settingsEvent.state));
                guild.setRights(settingsEvent.rights);

                // Read forum toggle
                boolean forumEnabled = this.packet.readBoolean();
                boolean wasForumEnabled = guild.hasForum();

                if (forumEnabled != wasForumEnabled) {
                    guild.setForum(forumEnabled);

                    if (!forumEnabled) {
                        // Delete all threads and comments for this guild
                        ForumThread.clearCacheForGuild(guildId);
                        deleteForumData(guildId);
                    }

                    // Invalidate caches
                    GuildForumDataComposer.invalidateUnreadCache(guildId);
                    GuildForumListEvent.invalidateActiveForumsCache();
                }

                Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(guild.getRoomId());
                if(room != null) {
                    room.refreshGuild(guild);
                }

                guild.needsUpdate = true;

                Emulator.getThreading().run(guild);
            }
        }
    }

    private void deleteForumData(int guildId) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            // Delete comments for all threads in this guild
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `guilds_forums_comments` WHERE `thread_id` IN (SELECT `id` FROM `guilds_forums_threads` WHERE `guild_id` = ?)")) {
                statement.setInt(1, guildId);
                statement.executeUpdate();
            }

            // Delete all threads for this guild
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `guilds_forums_threads` WHERE `guild_id` = ?")) {
                statement.setInt(1, guildId);
                statement.executeUpdate();
            }

            // Delete forum view records for this guild
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `guild_forum_views` WHERE `guild_id` = ?")) {
                statement.setInt(1, guildId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to delete forum data for guild " + guildId, e);
        }
    }
}
