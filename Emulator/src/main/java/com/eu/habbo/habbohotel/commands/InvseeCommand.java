package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.invsee.InvseeService;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.HabboInfo;

/** Staff: inspect another user's inventory and confiscate items if needed. */
public class InvseeCommand extends Command {
    public InvseeCommand() {
        super(InvseeService.PERMISSION_KEY,
                Emulator.getTexts().getValue("commands.keys.cmd_invsee", "invsee").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length != 2) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue("commands.generic.cmd_invsee.usage", ":invsee <username>"));
            return true;
        }

        HabboInfo target = InvseeService.resolveTarget(params[1]);
        if (target == null) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts()
                            .getValue("commands.generic.cmd_invsee.not_found", "Utente non trovato.")
                            .replace("%user%", params[1]));
            return true;
        }

        InvseeService.sendInventory(gameClient, target);
        return true;
    }
}
