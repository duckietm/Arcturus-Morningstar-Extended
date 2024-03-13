package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;

public class ConnectCameraCommand extends Command {
    public ConnectCameraCommand() {
        super("cmd_connect_camera", Emulator.getTexts().getValue("commands.keys.cmd_connect_camera").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        return false;
    }
}