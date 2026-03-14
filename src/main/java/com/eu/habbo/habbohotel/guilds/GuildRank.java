package com.eu.habbo.habbohotel.guilds;

public enum GuildRank {
    OWNER(0),
    ADMIN(1),
    MEMBER(2),
    REQUESTED(3),
    DELETED(4);

    public final int type;

    GuildRank(int type) {
        this.type = type;
    }

    public static GuildRank getRank(int type) {
        try {
            return values()[type];
        } catch (Exception e) {
            return MEMBER;
        }
    }
}
