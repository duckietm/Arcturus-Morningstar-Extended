package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.BssCommandPreferences.Flag;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

final class BssPreferenceCommand extends Command {
    private final Flag flag;

    BssPreferenceCommand(String permission, Flag flag) {
        super(permission, Emulator.getTexts().getValue("commands.keys." + permission).split(";"));
        this.flag = flag;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        int userId = gameClient.getHabbo().getHabboInfo().getId();
        boolean enabled = BssCommandPreferences.toggle(userId, flag);
        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue("commands.success." + permission + "." + (enabled ? "enabled" : "disabled")),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}
