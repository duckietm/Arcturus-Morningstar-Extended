package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.rooms.items.PostItStickyPoleOpenComposer;

public class MultiCommand extends Command {
    public MultiCommand() {
        super("cmd_multi", Emulator.getTexts().getValue("commands.keys.cmd_multi").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        gameClient.sendResponse(new PostItStickyPoleOpenComposer(null));
        return true;
    }
}
