package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

/**
 * {@code :apripoker} — announces the poker table in the current room with the poker badge image and a
 * clickable link. Texts live in {@code emulator_texts} under {@code notifica.pok.*} (housekeeping).
 */
public class PokerNotificationCommand extends Command {
    public PokerNotificationCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_apripoker", "apripoker;poker;aprilpoker").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();

        if (!HotelNotificationCommand.hasStaffRank(habbo)) {
            habbo.whisper(Emulator.getTexts().getValue("notifica.error.rank", "Non hai i permessi per usare questo comando."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) {
            habbo.whisper(Emulator.getTexts().getValue("notifica.error.room", "Devi trovarti nella zona poker."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        String extra = params.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(params, 1, params.length)) : "";
        HotelNotificationCommand.send(habbo, room, "pok", extra);
        habbo.whisper(Emulator.getTexts().getValue("notifica.poker.sent", "Poker annunciato all'hotel."), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
