package com.eu.habbo.habbohotel.guides;

public enum GuardianVoteType {
    FORWARDED(-1),
    WAITING(0),
    ACCEPTABLY(1),
    BADLY(2),
    AWFULLY(3),
    NOT_VOTED(4),
    SEARCHING(5);

    private final int type;

    GuardianVoteType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

}
