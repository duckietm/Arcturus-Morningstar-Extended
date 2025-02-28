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
                    newSpeed = Integer.parseInt(params[1]);
                } catch (Exception e) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_setspeed.invalid_amount"), RoomChatMessageBubbles.ALERT);
                    return true;
                }

                // First check against the config bounds
                int configMax = Emulator.getConfig().getInt("hotel.rollers.speed.maximum");
                if (newSpeed < -1 || newSpeed > configMax) {
                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_setspeed.bounds"), RoomChatMessageBubbles.ALERT);
                    return true;
                }

                // Enforce maximum speed of 10 regardless of config.
                if (newSpeed > 10) {
                    newSpeed = 10;
                    gameClient.getHabbo().whisper("Speed cannot be set above 10. Setting speed to 10.", RoomChatMessageBubbles.ALERT);
                }

                room.setRollerSpeed(newSpeed);

                gameClient.getHabbo().whisper(
                        Emulator.getTexts().getValue("commands.succes.cmd_setspeed")
                                .replace("%oldspeed%", oldSpeed + "")
                                .replace("%newspeed%", newSpeed + ""),
                        RoomChatMessageBubbles.ALERT
                );
                return true;
            }
        }
        return false;
    }
}
