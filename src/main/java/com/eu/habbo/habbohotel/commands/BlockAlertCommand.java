package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class BlockAlertCommand extends Command {
    public BlockAlertCommand() {
        super("cmd_blockalert", Emulator.getTexts().getValue("commands.keys.cmd_blockalert").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            gameClient.getHabbo().getHabboStats().blockStaffAlerts = !gameClient.getHabbo().getHabboStats().blockStaffAlerts;
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_blockalert").replace("%state%", (gameClient.getHabbo().getHabboStats().blockStaffAlerts ? Emulator.getTexts().getValue("generic.on") : Emulator.getTexts().getValue("generic.off"))), RoomChatMessageBubbles.ALERT);

            return true;
        }

        return false;
    }
}
