package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomAutoStackSupport;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.Locale;
import java.util.Set;

/** {@code :autostackheight [on|off]} — per-room automatic furniture stack-height adjustment (default off). */
public final class AutoStackHeightCommand extends Command {
    private static final Set<String> ON = Set.of("on", "si", "sì", "1", "attiva", "yes");
    private static final Set<String> OFF = Set.of("off", "no", "0", "disattiva");

    public AutoStackHeightCommand() {
        super(null, Emulator.getTexts().getValue("commands.keys.cmd_autostackheight", "autostackheight;autostack;altezzaauto").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Habbo habbo = gameClient.getHabbo();
        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (!room.isOwner(habbo) && !room.hasRights(habbo) && !habbo.hasPermission(Permission.ACC_ANYROOMOWNER)) {
            habbo.whisper("Solo il proprietario della stanza (o chi ha i diritti) può usare questo comando.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        boolean current = RoomAutoStackSupport.isEnabled(room);
        boolean next = !current;
        if (params.length >= 2) {
            String value = params[1].toLowerCase(Locale.ROOT);
            if (ON.contains(value)) next = true;
            else if (OFF.contains(value)) next = false;
        }

        RoomAutoStackSupport.set(room, next);
        habbo.whisper(next
                ? "Regolazione automatica dell'altezza dei Furni: attiva."
                : "Regolazione automatica dell'altezza dei Furni: disattivata.", RoomChatMessageBubbles.ALERT);
        return true;
    }
}
