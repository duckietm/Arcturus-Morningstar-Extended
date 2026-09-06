package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class ToggleTradeCommand extends Command {
    public ToggleTradeCommand() {
        super("cmd_toggle_trade", Emulator.getTexts().getValue("commands.keys.cmd_toggle_trade").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (!room.isOwner(gameClient.getHabbo())) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.room_owner_only"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        boolean enabled = room.getTradeMode() == 0;
        room.setTradeMode(enabled ? 2 : 0);
        room.setNeedsUpdate(true);
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue(enabled ? "commands.success.cmd_toggle_trade.enabled" : "commands.success.cmd_toggle_trade.disabled"),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}
