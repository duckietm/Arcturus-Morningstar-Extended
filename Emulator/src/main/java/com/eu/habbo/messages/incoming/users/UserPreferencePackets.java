package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.messages.ClientMessage;

/**
 * Field layouts and sanitizing rules of the official AIR 13 per-user preference packets
 * (SetChatPreferences 2506, SetOnlineIndicatorPreference 818, wired menu preferences 1226).
 * The ranges mirror HabboFreeFlowChat.sanitizeChat* and OtherSettingsView: an unknown chat mode
 * falls back to free flow, an unknown width or speed to normal, an unknown notification preference
 * to everyone.
 */
public final class UserPreferencePackets {
    public static final int CHAT_MODE_FREE_FLOW = 0;
    public static final int CHAT_MODE_LINE_BY_LINE = 1;
    public static final int CHAT_BUBBLE_WIDTH_NORMAL = 1;
    public static final int CHAT_SCROLL_SPEED_NORMAL = 1;
    public static final int ONLINE_INDICATOR_EVERYONE = 0;
    public static final int ONLINE_INDICATOR_NOBODY = 2;

    public record ChatPreferences(int chatMode, int chatBubbleWidth, int chatScrollSpeed) {}

    private UserPreferencePackets() {}

    public static int sanitizeChatMode(int value) {
        return value == CHAT_MODE_LINE_BY_LINE ? value : CHAT_MODE_FREE_FLOW;
    }

    public static int sanitizeChatBubbleWidth(int value) {
        return value >= 0 && value <= 2 ? value : CHAT_BUBBLE_WIDTH_NORMAL;
    }

    public static int sanitizeChatScrollSpeed(int value) {
        return value >= 0 && value <= 2 ? value : CHAT_SCROLL_SPEED_NORMAL;
    }

    public static int sanitizeOnlineIndicatorPreference(int value) {
        return value >= ONLINE_INDICATOR_EVERYONE && value <= ONLINE_INDICATOR_NOBODY
                ? value
                : ONLINE_INDICATOR_EVERYONE;
    }

    /** Official layout: a leading boolean the client always sends as false, then mode, width, speed. */
    public static ChatPreferences parseChatPreferences(ClientMessage packet) {
        packet.readBoolean();
        int chatMode = sanitizeChatMode(packet.readInt());
        int chatBubbleWidth = sanitizeChatBubbleWidth(packet.readInt());
        int chatScrollSpeed = sanitizeChatScrollSpeed(packet.readInt());
        return new ChatPreferences(chatMode, chatBubbleWidth, chatScrollSpeed);
    }

    public static int parseOnlineIndicatorPreference(ClientMessage packet) {
        return sanitizeOnlineIndicatorPreference(packet.readInt());
    }

    /**
     * Official layout: menu button, inspect button, play-test mode, a reserved int, wired whisper
     * disabled, show all notifications, ui style. Only the whisper switch is persisted here; the
     * other flags belong to the official wired menu the emulator does not model.
     */
    public static boolean parseWiredWhisperDisabled(ClientMessage packet) {
        packet.readBoolean();
        packet.readBoolean();
        packet.readBoolean();
        packet.readInt();
        boolean wiredWhisperDisabled = packet.readBoolean();
        packet.readBoolean();
        packet.readString();
        return wiredWhisperDisabled;
    }
}
