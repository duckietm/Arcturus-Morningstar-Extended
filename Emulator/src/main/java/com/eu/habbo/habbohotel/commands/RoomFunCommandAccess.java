package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

final class RoomFunCommandAccess {
    private static final int STAFF_RANK = 4;

    private RoomFunCommandAccess() {}

    static boolean requireOwnerOrStaff(Habbo habbo, Room room) {
        boolean staff = habbo.getHabboInfo().getRank() != null
                && habbo.getHabboInfo().getRank().getId() >= STAFF_RANK;
        if (staff || room.isOwner(habbo)) return true;

        habbo.whisper(
                Emulator.getTexts().getValue(
                        "commands.error.cmd_fun_room.permission",
                        "Solo il proprietario della stanza o lo staff può usare questo comando."),
                RoomChatMessageBubbles.ALERT);
        return false;
    }
}
