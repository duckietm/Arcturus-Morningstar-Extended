package com.eu.habbo.habbohotel.guilds.forums;

public enum ForumThreadState {
    OPEN(0),
    CLOSED(1),
    HIDDEN_BY_STAFF_MEMBER(10),
    HIDDEN_BY_GUILD_ADMIN(20);

    private int stateId;

    ForumThreadState(int stateId) {
        this.stateId = stateId;
    }

    public static ForumThreadState fromValue(int value) {
        for (ForumThreadState state : ForumThreadState.values()) {
            if (state.stateId == value) {
                return state;
            }
        }

        return CLOSED;
    }

    public int getStateId() {
        return this.stateId;
    }
}
