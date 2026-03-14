package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserHandItemComposer;

public class HandItemCommand extends Command {
    public HandItemCommand() {
        super("cmd_hand_item", Emulator.getTexts().getValue("commands.keys.cmd_hand_item").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 2) {
            try {
                if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                    int effectId = Integer.parseInt(params[1]);
                    gameClient.getHabbo().getRoomUnit().setHandItem(effectId);
                    gameClient.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserHandItemComposer(gameClient.getHabbo().getRoomUnit()).compose());
                }
            } catch (Exception e) {
                //Don't handle incorrect parse exceptions :P
            }
        }
        return true;
    }
}
