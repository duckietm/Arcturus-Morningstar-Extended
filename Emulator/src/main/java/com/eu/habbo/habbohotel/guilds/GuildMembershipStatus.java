package com.eu.habbo.habbohotel.guilds;

public enum GuildMembershipStatus {
    NOT_MEMBER(0),
    MEMBER(1),
    PENDING(2);

    private int status;

    GuildMembershipStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
