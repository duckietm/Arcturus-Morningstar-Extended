package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDataComposer;
import com.eu.habbo.messages.outgoing.users.UpdateUserLookComposer;


public class FacelessCommand extends Command {
    public FacelessCommand() {
        super("cmd_faceless", Emulator.getTexts().getValue("commands.keys.cmd_faceless").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            try {

                String[] figureParts = gameClient.getHabbo().getHabboInfo().getLook().split("\\.");

                for (String part : figureParts) {
                    if (part.startsWith("hd")) {
                        String[] headParts = part.split("-");

                        if (!headParts[1].equals("99999"))
                            headParts[1] = "99999";
                        else
                            break;

                        String newHead = "hd-" + headParts[1] + "-" + headParts[2];

                        gameClient.getHabbo().getHabboInfo().setLook(gameClient.getHabbo().getHabboInfo().getLook().replace(part, newHead));
                        gameClient.sendResponse(new UpdateUserLookComposer(gameClient.getHabbo()));
                        gameClient.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserDataComposer(gameClient.getHabbo()).compose());
                        return true;
                    }
                }

            } catch (Exception e) {

            }
        }

        return false;
    }
}
