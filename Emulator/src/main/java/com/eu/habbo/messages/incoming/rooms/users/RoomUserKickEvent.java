package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserWhisperComposer;
import com.eu.habbo.plugin.events.users.UserKickEvent;

public class RoomUserKickEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null)
            return;

        int userId = this.packet.readInt();

        if (!RoomUserInputGuard.isPositiveId(userId))
            return;

        Habbo target = room.getHabbo(userId);

        if (target == null)
            return;

        if (target.hasPermission(Permission.ACC_UNKICKABLE)) {
            this.client.sendResponse(new RoomUserWhisperComposer(new RoomChatMessage(Emulator.getTexts().getValue("commands.error.cmd_kick.unkickable").replace("%username%", target.getHabboInfo().getUsername()), this.client.getHabbo(), this.client.getHabbo(), RoomChatMessageBubbles.ALERT)));
            return;
        }

        boolean isStaff = this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)
                || this.client.getHabbo().hasPermission(Permission.ACC_AMBASSADOR);

        // A guest with rights must not be able to throw the owner out of their own room. Staff must: that is
        // what moderating a room means, and this check used to stop them too, silently.
        if (room.isOwner(target) && !isStaff) {
            this.whisper(target, "commands.error.cmd_kick.owner");
            return;
        }

        if (!room.hasRights(this.client.getHabbo()) && !isStaff) {
            this.whisper(target, "commands.error.cmd_kick.rights");
            return;
        }

        UserKickEvent event = new UserKickEvent(this.client.getHabbo(), target);
        Emulator.getPluginManager().fireEvent(event);

        if (event.isCancelled())
            return;

        room.kickHabbo(target, true);
        AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("SelfModKickSeen"));
    }

    /**
     * Tells the kicker why nothing happened.
     *
     * Every refusal here used to return in silence, so a permission problem and a broken button looked
     * identical. Falls back to the key itself when the hotel has no text for it, which is still more than
     * nothing.
     */
    private void whisper(Habbo target, String key) {
        String text = Emulator.getTexts().getValue(key, key).replace("%username%", target.getHabboInfo().getUsername());

        this.client.sendResponse(new RoomUserWhisperComposer(
                new RoomChatMessage(text, this.client.getHabbo(), this.client.getHabbo(), RoomChatMessageBubbles.ALERT)));
    }
}
