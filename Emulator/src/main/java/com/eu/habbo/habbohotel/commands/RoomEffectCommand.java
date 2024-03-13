package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

public class RoomEffectCommand extends Command {
    public RoomEffectCommand() {
        super("cmd_roomeffect", Emulator.getTexts().getValue("commands.keys.cmd_roomeffect").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_roomeffect.no_effect"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        try {
            int effectId = Integer.valueOf(params[1]);

            if (effectId >= 0) {
                Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
                for (Habbo habbo : room.getHabbos()) {
                    room.giveEffect(habbo, effectId, -1);
                }

                return true;
            } else {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_roomeffect.positive"), RoomChatMessageBubbles.ALERT);
                return true;
            }
        } catch (Exception e) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_roomeffect.numbers_only"), RoomChatMessageBubbles.ALERT);
            return true;
        }
    }
}
