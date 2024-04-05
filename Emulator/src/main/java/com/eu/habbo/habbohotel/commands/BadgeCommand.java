package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BadgeCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(BadgeCommand.class);

    public BadgeCommand() {
        super("cmd_badge", Emulator.getTexts().getValue("commands.keys.cmd_badge").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 1) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_badge.forgot_username"), RoomChatMessageBubbles.ALERT);
            return true;
        }
        if (params.length == 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_badge.forgot_badge").replace("%user%", params[1]), RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params.length == 3) {
            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(params[1]);

            if (habbo != null) {
                if (habbo.addBadge(params[2])) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_badge.given").replace("%user%", params[1]).replace("%badge%", params[2]), RoomChatMessageBubbles.ALERT);
                } else {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_badge.already_owned").replace("%user%", params[1]).replace("%badge%", params[2]), RoomChatMessageBubbles.ALERT);
                }

                return true;
            } else {
                HabboInfo habboInfo = HabboManager.getOfflineHabboInfo(params[1]);

                if (habboInfo == null) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_badge.unknown_user"), RoomChatMessageBubbles.ALERT);
                    return true;
                }

                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                    boolean found;

                    try (PreparedStatement statement = connection.prepareStatement("SELECT `badge_code` FROM `users_badges` WHERE `user_id` = ? AND `badge_code` = ? LIMIT 1")) {
                        statement.setInt(1, habboInfo.getId());
                        statement.setString(2, params[2]);
                        try (ResultSet set = statement.executeQuery()) {
                            found = set.next();
                        }
                    }

                    if (found) {
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_badge.already_owns").replace("%user%", params[1]).replace("%badge%", params[2]), RoomChatMessageBubbles.ALERT);
                        return true;
                    } else {
                        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users_badges (`id`, `user_id`, `slot_id`, `badge_code`) VALUES (null, ?, 0, ?)")) {
                            statement.setInt(1, habboInfo.getId());
                            statement.setString(2, params[2]);
                            statement.execute();
                        }

                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_badge.given").replace("%user%", params[1]).replace("%badge%", params[2]), RoomChatMessageBubbles.ALERT);
                        return true;
                    }
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            }
        }

        return true;
    }
}
