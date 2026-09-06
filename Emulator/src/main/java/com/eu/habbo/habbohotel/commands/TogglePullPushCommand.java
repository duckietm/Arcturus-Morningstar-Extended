package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class TogglePullPushCommand extends Command {
    private final boolean pull;

    public TogglePullPushCommand(boolean pull) {
        super(
                pull ? "cmd_toggle_pull" : "cmd_toggle_push",
                Emulator.getTexts().getValue(pull ? "commands.keys.cmd_toggle_pull" : "commands.keys.cmd_toggle_push").split(";"));
        this.pull = pull;
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        if (!room.isOwner(gameClient.getHabbo())) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.room_owner_only"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        boolean enabled = this.pull ? !room.isPullEnabled() : !room.isPushEnabled();
        if (this.pull) room.setPullEnabled(enabled);
        else room.setPushEnabled(enabled);

        String key = "commands.success." + (this.pull ? "cmd_toggle_pull." : "cmd_toggle_push.") + (enabled ? "enabled" : "disabled");
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue(key), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
