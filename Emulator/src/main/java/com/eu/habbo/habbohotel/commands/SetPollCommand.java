package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class SetPollCommand extends Command {
    public SetPollCommand() {
        super("cmd_set_poll", Emulator.getTexts().getValue("commands.keys.cmd_set_poll").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length >= 2) {
            if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                int pollId = -1;
                try {
                    pollId = Integer.valueOf(params[1]);
                } catch (Exception e) {

                }

                if (pollId >= 0) {
                    if (Emulator.getGameEnvironment().getPollManager().getPoll(pollId) != null) {
                        gameClient.getHabbo().getHabboInfo().getCurrentRoom().setPollId(pollId);
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_set_poll"), RoomChatMessageBubbles.ALERT);
                    } else {
                        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_set_poll.not_found"), RoomChatMessageBubbles.ALERT);
                    }
                } else {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_set_poll.invalid_number"), RoomChatMessageBubbles.ALERT);
                }
            }
        } else {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_set_poll.missing_arg"), RoomChatMessageBubbles.ALERT);
        }

        return true;
    }
}