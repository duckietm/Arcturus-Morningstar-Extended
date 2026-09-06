package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.AvatarHandItemSupport;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserHandItemComposer;
import com.eu.habbo.messages.outgoing.users.InClientLinkComposer;

public class HandItemCommand extends Command {
    public HandItemCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_hand_item").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 2 && params[1].equalsIgnoreCase("list")) {
            gameClient.sendResponse(new InClientLinkComposer("avatar-handitems/show"));
            return true;
        }

        if (params.length == 3 && params[1].equalsIgnoreCase("all")) {
            try {
                int handItemId = Integer.parseInt(params[2]);
                if (!AvatarHandItemSupport.isSupported(handItemId)) return true;
                if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() == null) return true;
                for (var habbo : gameClient.getHabbo().getHabboInfo().getCurrentRoom().getHabbos()) {
                    habbo.getRoomUnit().setHandItem(handItemId);
                    gameClient.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserHandItemComposer(habbo.getRoomUnit()).compose());
                }
            } catch (NumberFormatException ignored) { }
            return true;
        }

        if (params.length == 2) {
            try {
                if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                    int effectId = Integer.parseInt(params[1]);
                    if (!AvatarHandItemSupport.isSupported(effectId)) {
                        return true;
                    }
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
