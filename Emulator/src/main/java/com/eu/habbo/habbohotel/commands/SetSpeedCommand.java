package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class SetSpeedCommand extends Command {
    public SetSpeedCommand() {
        super("cmd_setspeed", Emulator.getTexts().getValue("commands.keys.cmd_setspeed").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            if (gameClient.getHabbo().getHabboInfo().getCurrentRoom().hasRights(gameClient.getHabbo())) {
                Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();

                int oldSpeed = room.getRollerSpeed();
                int newSpeed;

                try {
                    newSpeed = Integer.valueOf(params[1]);
                } catch (Exception e) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_setspeed.invalid_amount"), RoomChatMessageBubbles.ALERT);
                    return true;
                }

                if (newSpeed < -1 || newSpeed > Emulator.getConfig().getInt("hotel.rollers.speed.maximum")) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_setspeed.bounds"), RoomChatMessageBubbles.ALERT);
                    return true;
                }

                room.setRollerSpeed(newSpeed);

                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_setspeed").replace("%oldspeed%", oldSpeed + "").replace("%newspeed%", newSpeed + ""), RoomChatMessageBubbles.ALERT);
                return true;
            }
        }
        return false;
    }
}
