package com.eu.habbo.messages.incoming.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.commands.AvailableCommandsComposer;

/** Sends the authoritative, rank-filtered command and alias list on demand. */
public final class RequestAvailableCommandsEvent extends MessageHandler {

    @Override
    public void handle() {
        if (this.client == null || this.client.getHabbo() == null) return;

        this.client.sendResponse(new AvailableCommandsComposer(
                Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(
                        this.client.getHabbo().getHabboInfo().getRank().getId())));
    }
}
