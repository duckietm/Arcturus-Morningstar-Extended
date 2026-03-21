package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PrefixBlacklistCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixBlacklistCommand.class);

    public PrefixBlacklistCommand() {
        super("cmd_prefix_blacklist", Emulator.getTexts().getValue("commands.keys.cmd_prefix_blacklist").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_prefix_blacklist.usage"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String action = params[1].toLowerCase();

        if (action.equals("list")) {
            StringBuilder sb = new StringBuilder();
            sb.append(Emulator.getTexts().getValue("commands.succes.cmd_prefix_blacklist.header")).append("\r");

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT word FROM custom_prefix_blacklist ORDER BY word")) {
                try (ResultSet set = statement.executeQuery()) {
                    int count = 0;
                    while (set.next()) {
                        sb.append("- ").append(set.getString("word")).append("\r");
                        count++;
                    }
                    if (count == 0) {
                        sb.append(Emulator.getTexts().getValue("commands.succes.cmd_prefix_blacklist.empty"));
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Error listing prefix blacklist", e);
            }

            gameClient.getHabbo().whisper(sb.toString(), RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params.length < 3) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_prefix_blacklist.usage"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String word = params[2].toLowerCase().trim();

        if (word.isEmpty()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_prefix_blacklist.empty_word"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (action.equals("add")) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO custom_prefix_blacklist (word) VALUES (?)")) {
                statement.setString(1, word);
                statement.execute();
            } catch (SQLException e) {
                LOGGER.error("Error adding prefix blacklist word", e);
            }

            gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.succes.cmd_prefix_blacklist.added").replace("%word%", word),
                RoomChatMessageBubbles.ALERT
            );
        } else if (action.equals("remove")) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM custom_prefix_blacklist WHERE word = ?")) {
                statement.setString(1, word);
                statement.execute();
            } catch (SQLException e) {
                LOGGER.error("Error removing prefix blacklist word", e);
            }

            gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.succes.cmd_prefix_blacklist.removed").replace("%word%", word),
                RoomChatMessageBubbles.ALERT
            );
        } else {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_prefix_blacklist.usage"), RoomChatMessageBubbles.ALERT);
        }

        return true;
    }
}
