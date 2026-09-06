package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;

public class PunchCommand extends Command {
    public PunchCommand() {
        super("cmd_punch", Emulator.getTexts().getValue("commands.keys.cmd_punch").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (params.length != 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_punch.usage"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        Habbo target = room.getHabbo(params[1]);
        if (target == null) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.error.target_not_found").replace("%user%", params[1]),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }
        if (target == gameClient.getHabbo()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.target_self"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String sender = gameClient.getHabbo().getHabboInfo().getUsername();
        String receiver = target.getHabboInfo().getUsername();
        room.giveEffect(gameClient.getHabbo(), 33, 3);
        room.giveEffect(target, 34, 3);
        room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage(
                        Emulator.getTexts().getValue("commands.action.punch.sender").replace("%target%", receiver),
                        gameClient.getHabbo(), gameClient.getHabbo(), RoomChatMessageBubbles.RED))
                .compose());
        room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage(
                        Emulator.getTexts().getValue("commands.action.punch.receiver").replace("%sender%", sender),
                        target, target, RoomChatMessageBubbles.RED))
                .compose());
        return true;
    }
}
