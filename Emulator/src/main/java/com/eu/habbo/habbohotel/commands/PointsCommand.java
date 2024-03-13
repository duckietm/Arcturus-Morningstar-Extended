package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

public class PointsCommand extends Command {
    public PointsCommand() {
        super("cmd_points", Emulator.getTexts().getValue("commands.keys.cmd_points").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length >= 3) {
            Habbo habbo = Emulator.getGameServer().getGameClientManager().getHabbo(params[1]);

            if (habbo != null) {
                try {
                    int type = Emulator.getConfig().getInt("seasonal.primary.type");

                    if (params.length == 4) {
                        try {
                            type = Integer.valueOf(params[3]);
                        } catch (Exception e) {
                            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_points.invalid_type").replace("%types%", Emulator.getConfig().getValue("seasonal.types").replace(";", ", ")), RoomChatMessageBubbles.ALERT);
                            return true;
                        }
                    }

                    int amount;

                    try {
                        amount = Integer.valueOf(params[2]);
                    } catch (Exception e) {
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_points.invalid_amount"), RoomChatMessageBubbles.ALERT);
                        return true;
                    }

                    if (amount != 0) {
                        habbo.givePoints(type, amount);

                        if (habbo.getHabboInfo().getCurrentRoom() != null)
                            habbo.whisper(Emulator.getTexts().getValue("commands.generic.cmd_points.received").replace("%amount%", amount + "").replace("%type%", Emulator.getTexts().getValue("seasonal.name." + type)), RoomChatMessageBubbles.ALERT);
                        else
                            habbo.alert(Emulator.getTexts().getValue("commands.generic.cmd_points.received").replace("%amount%", amount + "").replace("%type%", Emulator.getTexts().getValue("seasonal.name." + type)));

                        // habbo.getClient().sendResponse(new UserPointsComposer(habbo.getHabboInfo().getCurrencyAmount(type), amount, type));

                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_points.send").replace("%amount%", amount + "").replace("%user%", params[1]).replace("%type%", Emulator.getTexts().getValue("seasonal.name." + type)), RoomChatMessageBubbles.ALERT);

                    } else {
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_points.invalid_amount"), RoomChatMessageBubbles.ALERT);
                    }
                } catch (NumberFormatException e) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_points.invalid_amount"), RoomChatMessageBubbles.ALERT);
                }
            } else {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_points.user_offline").replace("%user%", params[1]), RoomChatMessageBubbles.ALERT);
            }
        } else {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_points.invalid_amount"), RoomChatMessageBubbles.ALERT);
        }
        return true;
    }
}
