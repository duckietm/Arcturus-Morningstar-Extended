package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.WiredPlatform;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredTextPlaceholderUtil;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserWhisperComposer;

/**
 * The optional message a user-targeting box carries ("mute user", "sit", "lay", "fast walk"): a
 * whisper to each affected user with the placeholders the show-message box expands. Kept in one
 * place so the posture boxes, which saved the message and never sent it, say the same thing "mute
 * user" always did.
 */
final class WiredEffectUserMessage {
    private WiredEffectUserMessage() {}

    static void whisper(WiredContext ctx, Habbo habbo, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        if (habbo == null || habbo.getHabboInfo() == null || habbo.getClient() == null) {
            return;
        }

        String username = habbo.getHabboInfo().getUsername();
        String text = message.replace("%user%", username == null ? "" : username);

        // The hotel-wide counters cost a lookup each, so they are only resolved when asked for.
        GameEnvironment environment = WiredPlatform.gameEnvironment();
        if (text.contains("%online_count%")) {
            int online =
                    (environment == null) ? 0 : environment.getHabboManager().getOnlineCount();
            text = text.replace("%online_count%", online + "");
        }
        if (text.contains("%room_count%")) {
            int rooms = (environment == null)
                    ? 0
                    : environment.getRoomManager().getActiveRooms().size();
            text = text.replace("%room_count%", rooms + "");
        }

        text = WiredTextPlaceholderUtil.applyUsernamePlaceholders(ctx, text);
        habbo.getClient()
                .sendResponse(new RoomUserWhisperComposer(
                        new RoomChatMessage(text, habbo, habbo, RoomChatMessageBubbles.WIRED)));
    }
}
