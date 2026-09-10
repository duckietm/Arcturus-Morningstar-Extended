package com.eu.habbo.habbohotel.rooms.competition;

/**
 * The result codes the official client reads out of the submit and the voting packet. They pick the
 * texts the window shows and which button it offers, so they are not ours to renumber: the names
 * come from `roomcompetition.caption.submit.<n>` and `roomcompetition.caption.vote.<n>`.
 */
public final class RoomCompetitionResult {
    /** "Your room is now part of the competition!" — the entry is in. */
    public static final int SUBMITTED = 0;

    /** The room qualifies; the button submits it. */
    public static final int READY = 1;

    /** "Are you sure you want to submit?" — the button confirms. */
    public static final int CONFIRM = 2;

    /** The room is missing furniture the competition requires; the window lists it. */
    public static final int MISSING_FURNI = 3;

    /** "Your room door is not open" — visitors could not get in to vote. */
    public static final int DOOR_CLOSED = 4;

    /** The room cannot enter; the button opens the navigator to pick another one. */
    public static final int ROOM_NOT_ELIGIBLE = 5;

    /** First contact: the rules, and a button that accepts them. */
    public static final int RULES = 6;

    /** Voting: "Ouch! You cannot vote yet." */
    public static final int VOTE_NOT_ALLOWED = 1;

    /** Voting: the room can be voted for, if the visitor has votes left. */
    public static final int VOTE_ALLOWED = 2;

    private RoomCompetitionResult() {}
}
