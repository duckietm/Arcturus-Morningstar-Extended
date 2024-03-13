package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdateWordFilterCommand extends Command {
    public UpdateWordFilterCommand() {
        super("cmd_update_wordfilter", Emulator.getTexts().getValue("commands.keys.update_wordfilter").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getGameEnvironment().getWordFilter().reload();

        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_wordfilter"), RoomChatMessageBubbles.ALERT);

        return true;
    }
}
