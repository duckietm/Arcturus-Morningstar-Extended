package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.core.TextsManager;
import com.eu.habbo.database.SqlQueries;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.outgoing.users.UserDataComposer;

public class GiveRespectPointsCommand extends Command {
    private static final int DEFAULT_MAX_GRANT = 1_000_000;
    private final TextsManager texts;
    private final ConfigurationManager config;
    private final HabboManager habboManager;

    public GiveRespectPointsCommand(TextsManager texts, ConfigurationManager config, HabboManager habboManager) {
        super("cmd_give_respect_points",
                texts.getValue("commands.keys.cmd_give_respect_points").split(";"));
        this.texts = texts;
        this.config = config;
        this.habboManager = habboManager;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params.length != 3) {
            whisper(gameClient, "commands.error.cmd_give_respect_points.usage");
            return true;
        }

        final int amount;
        try {
            amount = Integer.parseInt(params[2]);
        } catch (NumberFormatException exception) {
            whisper(gameClient, "commands.error.cmd_give_respect_points.invalid_amount");
            return true;
        }

        int maxGrant = Math.max(1, this.config.getInt("hotel.respect.grant.max", DEFAULT_MAX_GRANT));
        if (amount <= 0 || amount > maxGrant) {
            gameClient.getHabbo().whisper(
                    this.texts.getValue("commands.error.cmd_give_respect_points.invalid_amount")
                            .replace("%max%", Integer.toString(maxGrant)),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        HabboInfo info = HabboManager.getOfflineHabboInfo(params[1]);
        if (info == null) {
            gameClient.getHabbo().whisper(
                    this.texts.getValue("commands.error.cmd_give_respect_points.user_not_found")
                            .replace("%user%", params[1]),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        Habbo online = this.habboManager.getHabbo(info.getId());
        int current = online != null
                ? online.getHabboStats().respectPointsToGive
                : SqlQueries.queryOne(
                        "SELECT daily_respect_points FROM users_settings WHERE user_id = ? LIMIT 1",
                        row -> row.getInt("daily_respect_points"), info.getId()).orElse(0);
        int updated = (int) Math.min(Integer.MAX_VALUE, (long) current + amount);

        if (SqlQueries.update(
                "UPDATE users_settings SET daily_respect_points = ? WHERE user_id = ? LIMIT 1",
                updated, info.getId()) == 0) {
            whisper(gameClient, "commands.error.cmd_give_respect_points.storage");
            return true;
        }

        if (online != null) {
            online.getHabboStats().respectPointsToGive = updated;
            online.getClient().sendResponse(new UserDataComposer(online));
            String received = this.texts.getValue("commands.generic.cmd_give_respect_points.received")
                    .replace("%amount%", Integer.toString(amount))
                    .replace("%total%", Integer.toString(updated));
            if (online.getHabboInfo().getCurrentRoom() != null) {
                online.whisper(received, RoomChatMessageBubbles.ALERT);
            } else {
                online.alert(received);
            }
        }

        gameClient.getHabbo().whisper(
                this.texts.getValue("commands.success.cmd_give_respect_points")
                        .replace("%amount%", Integer.toString(amount))
                        .replace("%user%", info.getUsername())
                        .replace("%total%", Integer.toString(updated)),
                RoomChatMessageBubbles.ALERT);
        return true;
    }

    private void whisper(GameClient gameClient, String key) {
        gameClient.getHabbo().whisper(
                this.texts.getValue(key), RoomChatMessageBubbles.ALERT);
    }
}
