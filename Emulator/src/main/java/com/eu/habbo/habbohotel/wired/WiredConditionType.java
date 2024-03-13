package com.eu.habbo.habbohotel.wired;

public enum WiredConditionType {
    MATCH_SSHOT(0),
    FURNI_HAVE_HABBO(1),
    TRIGGER_ON_FURNI(2),
    TIME_MORE_THAN(3),
    TIME_LESS_THAN(4),
    USER_COUNT(5),
    ACTOR_IN_TEAM(6),
    FURNI_HAS_FURNI(7),
    STUFF_IS(8),
    ACTOR_IN_GROUP(10),
    ACTOR_WEARS_BADGE(11),
    ACTOR_WEARS_EFFECT(12),
    NOT_MATCH_SSHOT(13),
    NOT_FURNI_HAVE_HABBO(14),
    NOT_ACTOR_ON_FURNI(15),
    NOT_USER_COUNT(16),
    NOT_ACTOR_IN_TEAM(17),
    NOT_FURNI_HAVE_FURNI(18),
    NOT_STUFF_IS(19),
    NOT_ACTOR_IN_GROUP(21),
    NOT_ACTOR_WEARS_BADGE(22),
    NOT_ACTOR_WEARS_EFFECT(23),
    DATE_RANGE(24),
    ACTOR_HAS_HANDITEM(25);

    public final int code;

    WiredConditionType(int code) {
        this.code = code;
    }
}
