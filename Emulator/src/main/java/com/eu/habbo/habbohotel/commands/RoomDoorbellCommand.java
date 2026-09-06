package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomState;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.RoomSettingsUpdatedComposer;

import java.util.Locale;

/**
 * {@code :chiudistanza} puts the current room on the doorbell, {@code :apristanza} opens it again.
 * Available to arbitri/staff (same rank gate as {@code :notifica}) even without room rights.
 */
public class RoomDoorbellCommand extends Command {
    private final boolean close;

    private RoomDoorbellCommand(boolean close, String key, String fallbackKeys) {
        super(null, Emulator.getTexts().getValue(key, fallbackKeys).split(";"));
        this.close = close;
    }

    public static RoomDoorbellCommand close() {
        return new RoomDoorbellCommand(true, "commands.keys.cmd_chiudistanza", "chiudistanza;campanello;closeroom");
    }

    public static RoomDoorbellCommand open() {
        return new RoomDoorbellCommand(false, "commands.keys.cmd_apristanza", "apristanza;apri;openroom");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        Room room = habbo.getHabboInfo().getCurrentRoom();

        if (room == null) return true;

        boolean allowed = HotelNotificationCommand.hasStaffRank(habbo) || room.isOwner(habbo) || room.hasRights(habbo);
        if (!allowed) {
            habbo.whisper(Emulator.getTexts().getValue("notifica.error.rank", "Non hai i permessi per usare questo comando."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        // ":chiudistanza aperta" / ":apristanza campanello" also work
        boolean shouldClose = this.close;
        if (params.length >= 2) {
            String value = params[1].toLowerCase(Locale.ROOT);
            if (value.startsWith("apri") || value.equals("open") || value.equals("off")) shouldClose = false;
            else if (value.startsWith("chiud") || value.startsWith("campan") || value.equals("on")) shouldClose = true;
        }

        room.setState(shouldClose ? RoomState.LOCKED : RoomState.OPEN);
        Emulator.getThreading().run(room);
        room.sendComposer(new RoomSettingsUpdatedComposer(room).compose());

        habbo.whisper(
                shouldClose
                        ? Emulator.getTexts().getValue("room.doorbell.on", "Campanello attivato: gli utenti dovranno suonare per entrare.")
                        : Emulator.getTexts().getValue("room.doorbell.off", "Stanza aperta: chiunque può entrare."),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}
