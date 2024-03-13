package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.outgoing.catalog.*;
import com.eu.habbo.messages.outgoing.catalog.marketplace.MarketplaceConfigComposer;

public class UpdateCalendarCommand extends Command {

    public UpdateCalendarCommand() {
        super("cmd_update_calendar", Emulator.getTexts().getValue("commands.keys.cmd_update_calendar").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Emulator.getGameEnvironment().getCalendarManager().reload();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.success.cmd_update_calendar"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
