package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdateConfigCommand extends Command {
    public UpdateConfigCommand() {
        super("cmd_update_config", Emulator.getTexts().getValue("commands.keys.cmd_update_config").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getConfig().reload();

        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_config"), RoomChatMessageBubbles.ALERT);

        return true;
    }
}
