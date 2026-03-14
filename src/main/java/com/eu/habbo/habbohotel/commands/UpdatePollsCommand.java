package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdatePollsCommand extends Command {
    public UpdatePollsCommand() {
        super("cmd_update_polls", Emulator.getTexts().getValue("commands.keys.cmd_update_polls").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getGameEnvironment().getPollManager().loadPolls();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_polls"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}