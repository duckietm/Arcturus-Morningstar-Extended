package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

public class PixelCommand extends Command {
    public PixelCommand() {
        super("cmd_duckets", Emulator.getTexts().getValue("commands.keys.cmd_duckets").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 3) {
            Habbo habbo = Emulator.getGameServer().getGameClientManager().getHabbo(params[1]);

            if (habbo != null) {
                try {
                    if (Integer.parseInt(params[2]) != 0) {
                        habbo.givePixels(Integer.parseInt(params[2]));
                        if (habbo.getHabboInfo().getCurrentRoom() != null)
                            habbo.whisper(Emulator.getTexts().getValue("commands.generic.cmd_duckets.received").replace("%amount%", Integer.valueOf(params[2]) + ""), RoomChatMessageBubbles.ALERT);
                        else
                            habbo.alert(Emulator.getTexts().getValue("commands.generic.cmd_duckets.received").replace("%amount%", Integer.valueOf(params[2]) + ""));

                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_duckets.send").replace("%amount%", Integer.valueOf(params[2]) + "").replace("%user%", params[1]), RoomChatMessageBubbles.ALERT);

                    } else {
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_duckets.invalid_amount"), RoomChatMessageBubbles.ALERT);
                    }
                } catch (NumberFormatException e) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_duckets.invalid_amount"), RoomChatMessageBubbles.ALERT);
                }
            } else {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_duckets.user_offline").replace("%user%", params[1]), RoomChatMessageBubbles.ALERT);
            }
        } else {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_duckets.invalid_amount"), RoomChatMessageBubbles.ALERT);
        }
        return true;
    }
}
