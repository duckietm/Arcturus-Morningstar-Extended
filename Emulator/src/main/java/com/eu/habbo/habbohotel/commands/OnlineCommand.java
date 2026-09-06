package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.users.OnlineUsersComposer;

/** ":online" — opens the searchable online users window (CUSTOM packet 10088) instead of a plain alert. */
public class OnlineCommand extends Command {
    public OnlineCommand() {
        super("cmd_online", new String[] {"online", "on", "utenti", "connessi"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        gameClient.sendResponse(new OnlineUsersComposer(gameClient.getHabbo()));
        return true;
    }
}
