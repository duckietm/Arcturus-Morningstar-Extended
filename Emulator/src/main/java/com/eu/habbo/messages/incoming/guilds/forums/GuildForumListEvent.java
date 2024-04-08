package com.eu.habbo.messages.incoming.guilds.forums;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.forums.GuildForumListComposer;
import com.eu.habbo.messages.outgoing.handshake.ConnectionErrorComposer;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class GuildForumListEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildForumListEvent.class);

    @Override
    public void handle() throws Exception {
        int mode = this.packet.readInt();
        int offset = this.packet.readInt();
        int amount = this.packet.readInt();

        Set<Guild> guilds = null;
        switch (mode) {
            case 0: // most active
                guilds = getActiveForums();
                break;

            case 1: // most viewed
                guilds = Emulator.getGameEnvironment().getGuildManager().getMostViewed();
                break;

            case 2: // my groups
                guilds = getMyForums(this.client.getHabbo().getHabboInfo().getId());
                break;
        }

        if (guilds != null) {
            this.client.sendResponse(new GuildForumListComposer(guilds, this.client.getHabbo(), mode, offset));
        }
    }

    private THashSet<Guild> getActiveForums() {
        THashSet<Guild> guilds = new THashSet<Guild>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT `guilds`.`id`, SUM(`guilds_forums_threads`.`posts_count`) AS `post_count` " +
                "FROM `guilds_forums_threads` " +
                "LEFT JOIN `guilds` ON `guilds`.`id` = `guilds_forums_threads`.`guild_id` " +
                "WHERE `guilds`.`read_forum` = 'EVERYONE' AND `guilds_forums_threads`.`created_at` > ? " +
                "GROUP BY `guilds`.`id` " +
                "ORDER BY `post_count` DESC LIMIT 100")) {
            statement.setInt(1, Emulator.getIntUnixTimestamp() - 7 * 24 * 60 * 60);
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(set.getInt("id"));

                if (guild != null) {
                    guilds.add(guild);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
            this.client.sendResponse(new ConnectionErrorComposer(500));
        }

        return guilds;
    }

    private THashSet<Guild> getMyForums(int userId) {
        THashSet<Guild> guilds = new THashSet<Guild>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT `guilds`.`id` FROM `guilds_members` " +
                "LEFT JOIN `guilds` ON `guilds`.`id` = `guilds_members`.`guild_id` " +
                "WHERE `guilds_members`.`user_id` = ? AND `guilds`.`forum` = '1'")) {
            statement.setInt(1, userId);
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(set.getInt("id"));

                if (guild != null) {
                    guilds.add(guild);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
            this.client.sendResponse(new ConnectionErrorComposer(500));
        }

        return guilds;
    }
}