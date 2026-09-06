package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.inventory.InventoryBotsComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;

import java.util.ArrayList;
import java.util.List;

public class EmptyBotsInventoryCommand extends Command {
    public EmptyBotsInventoryCommand() {
        super("cmd_empty_bots", Emulator.getTexts().getValue("commands.keys.cmd_empty_bots").split(";"));
    }

    private static String verifyMessage(String[] params) {
        String alias = params.length > 0 && params[0] != null && !params[0].isEmpty() ? params[0] : "emptybots";
        return Emulator.getTexts().getValue("commands.succes.cmd_empty_bots.verify")
                .replace("%generic.yes%", Emulator.getTexts().getValue("generic.yes"))
                .replace("%command%", alias)
                .replace(":emptybots ", ":" + alias + " ");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 1 || (params.length >= 2 && !EmptyInventoryCommand.isConfirmation(params[1]))) {
            if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                if (gameClient.getHabbo().getHabboInfo().getCurrentRoom().getUserCount() > 10) {
                    gameClient.getHabbo().alert(verifyMessage(params));
                } else {
                    gameClient.getHabbo().whisper(verifyMessage(params), RoomChatMessageBubbles.ALERT);
                }
            }

            return true;
        }

        if (params.length >= 2 && EmptyInventoryCommand.isConfirmation(params[1])) {

            Habbo habbo = (params.length == 3 && gameClient.getHabbo().hasPermission(Permission.ACC_EMPTY_OTHERS)) ? Emulator.getGameEnvironment().getHabboManager().getHabbo(params[2]) : gameClient.getHabbo();

            if (habbo != null) {
                List<Bot> bots = new ArrayList<>(habbo.getInventory().getBotsComponent().getBots().values());
                habbo.getInventory().getBotsComponent().getBots().clear();
                bots.forEach(object -> Emulator.getGameEnvironment().getBotManager().deleteBot(object));

                habbo.getClient().sendResponse(new InventoryRefreshComposer());
                habbo.getClient().sendResponse(new InventoryBotsComposer(habbo));

                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_empty_bots.cleared").replace("%username%", habbo.getHabboInfo().getUsername()), RoomChatMessageBubbles.ALERT);
            } else {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_empty_bots"), RoomChatMessageBubbles.ALERT);
            }
        }

        return true;
    }
}
