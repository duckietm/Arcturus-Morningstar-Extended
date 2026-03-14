package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdateChatBubblesCommand extends Command {

    public UpdateChatBubblesCommand() {
        super("cmd_update_chat_bubbles", Emulator.getTexts().getValue("commands.keys.cmd_update_chat_bubbles").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        RoomChatMessageBubbles.removeDynamicBubbles();
        Emulator.getGameEnvironment().getRoomChatBubbleManager().reload();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.success.cmd_update_chat_bubbles"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
