package com.eu.habbo.habbohotel.wired;

public enum WiredEffectType {
    TOGGLE_STATE(0),
    RESET_TIMERS(1),
    MATCH_SSHOT(3),
    MOVE_ROTATE(4),
    GIVE_SCORE(6),
    SHOW_MESSAGE(7),
    TELEPORT(8),
    JOIN_TEAM(9),
    LEAVE_TEAM(10),
    CHASE(11),
    FLEE(12),
    MOVE_DIRECTION(13),
    GIVE_SCORE_TEAM(14),
    TOGGLE_RANDOM(15),
    MOVE_FURNI_TO(16),
    GIVE_REWARD(17),
    CALL_STACKS(18),
    KICK_USER(19),
    MUTE_TRIGGER(20),
    BOT_TELEPORT(21),
    BOT_MOVE(22),
    BOT_TALK(23),
    BOT_GIVE_HANDITEM(24),
    BOT_FOLLOW_AVATAR(25),
    BOT_CLOTHES(26),
    BOT_TALK_TO_AVATAR(27);

    public final int code;

    WiredEffectType(int code) {
        this.code = code;
    }
}
