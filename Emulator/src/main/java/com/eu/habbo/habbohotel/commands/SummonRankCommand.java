package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.ForwardToRoomComposer;

import java.util.Map;

public class SummonRankCommand extends Command {
    public SummonRankCommand() {
        super("cmd_summonrank", Emulator.getTexts().getValue("commands.keys.cmd_summonrank").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        int minRank;

        if (params.length >= 2) {
            try {
                minRank = Integer.parseInt(params[1]);
            } catch (Exception e) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.generic.cmd_summonrank.error"), RoomChatMessageBubbles.ALERT);
                return true;
            }

            for (Map.Entry<Integer, Habbo> set : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().entrySet()) {
                if (set.getValue().getHabboInfo().getRank().getId() >= minRank) {
                    if (set.getValue() == gameClient.getHabbo())
                        continue;

                    if (set.getValue().getHabboInfo().getCurrentRoom() == gameClient.getHabbo().getHabboInfo().getCurrentRoom())
                        continue;

                    Room room = set.getValue().getHabboInfo().getCurrentRoom();
                    if (room != null) {
                        Emulator.getGameEnvironment().getRoomManager().logExit(set.getValue());

                        room.removeHabbo(set.getValue(), true);

                        set.getValue().getHabboInfo().setCurrentRoom(null);
                    }

                    Emulator.getGameEnvironment().getRoomManager().enterRoom(set.getValue(), gameClient.getHabbo().getHabboInfo().getCurrentRoom().getId(), "", true);

                    set.getValue().getClient().sendResponse(new ForwardToRoomComposer(gameClient.getHabbo().getHabboInfo().getCurrentRoom().getId()));

                }
            }
        }

        return true;
    }
}
