package com.eu.habbo.habbohotel.habboway;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * In-memory bookkeeping of the Habbo Way quiz: the question set handed to
 * each user (so PostQuizAnswers is graded against what was actually asked)
 * and the retry lockout that follows a submitted attempt.
 */
public final class HabboWayQuizSessions {

    /** One attempt, then the quiz is locked for two hours. */
    public static final long RETRY_LOCKOUT_MS = 2L * 60L * 60L * 1000L;

    private static final HabboWayQuizSessions INSTANCE =
            new HabboWayQuizSessions(System::currentTimeMillis, new Random());

    private final Map<Integer, int[]> pendingQuestions = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lockedUntil = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final Random random;

    HabboWayQuizSessions(LongSupplier clock, Random random) {
        this.clock = clock;
        this.random = random;
    }

    public static HabboWayQuizSessions getInstance() {
        return INSTANCE;
    }

    public boolean isLocked(int userId) {
        Long until = this.lockedUntil.get(userId);
        if (until == null) {
            return false;
        }
        if (until <= this.clock.getAsLong()) {
            this.lockedUntil.remove(userId, until);
            return false;
        }
        return true;
    }

    /**
     * Starts an attempt: returns the question ids to send, or an empty array
     * while the user is still locked out from a previous attempt.
     */
    public int[] start(int userId) {
        if (this.isLocked(userId)) {
            return new int[0];
        }
        int[] questions = HabboWayQuiz.pickQuestions(this.random);
        this.pendingQuestions.put(userId, questions);
        return questions;
    }

    /** The question set the user is currently answering, or null. */
    public int[] pending(int userId) {
        return this.pendingQuestions.get(userId);
    }

    /** Closes the attempt and starts the retry lockout. */
    public void finish(int userId) {
        this.pendingQuestions.remove(userId);
        this.lockedUntil.put(userId, this.clock.getAsLong() + RETRY_LOCKOUT_MS);
    }

    public void forget(int userId) {
        this.pendingQuestions.remove(userId);
        this.lockedUntil.remove(userId);
    }
}
