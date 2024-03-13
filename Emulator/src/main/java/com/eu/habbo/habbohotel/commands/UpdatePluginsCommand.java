package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdatePluginsCommand extends Command {
    public UpdatePluginsCommand() {
        super("cmd_update_plugins", Emulator.getTexts().getValue("commands.keys.cmd_update_plugins").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getPluginManager().reload();

        gameClient.getHabbo().whisper("This is an unsafe command and could possibly lead to memory leaks.\rIt is recommended to restart the emulator in order to reload plugins.");
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_plugins").replace("%count%", Emulator.getPluginManager().getPlugins().size() + ""), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
