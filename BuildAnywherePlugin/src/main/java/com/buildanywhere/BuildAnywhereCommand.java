package com.buildanywhere;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class BuildAnywhereCommand extends Command {

    public BuildAnywhereCommand() {
        super("cmd_build_anywhere", Emulator.getTexts().getValue("commands.keys.cmd_build_anywhere").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        int userId = gameClient.getHabbo().getHabboInfo().getId();

        if (BuildAnywherePlugin.hasEnabled(userId)) {
            BuildAnywherePlugin.removeUser(userId);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_build_anywhere.disabled"), RoomChatMessageBubbles.ALERT);
        } else {
            BuildAnywherePlugin.addUser(userId);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_build_anywhere.enabled"), RoomChatMessageBubbles.ALERT);
        }
        return true;
    }
}
