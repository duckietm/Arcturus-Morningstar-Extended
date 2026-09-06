package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.users.InClientLinkComposer;

/** Opens the hotel-wide Battle Banzai timing panel. The panel asks the server for the values itself. */
public class BanzaiSpeedCommand extends Command {
    public BanzaiSpeedCommand() {
        super("cmd_banzai_speed", Emulator.getTexts()
                .getValue("commands.keys.cmd_banzai_speed", "banzai;velocitabanzai")
                .split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        gameClient.sendResponse(new InClientLinkComposer("banzai/toggle"));
        return true;
    }
}
