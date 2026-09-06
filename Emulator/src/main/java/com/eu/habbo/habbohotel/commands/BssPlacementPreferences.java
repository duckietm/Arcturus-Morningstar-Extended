package com.eu.habbo.habbohotel.commands;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Session-scoped placement overrides used by :placex, :forceheight and :forcerot. */
public final class BssPlacementPreferences {
    private static final Map<Integer, State> STATES = new ConcurrentHashMap<>();

    private BssPlacementPreferences() {}

    public static void setBatch(int userId, int count, double height) {
        state(userId).setBatch(count, height);
    }

    public static void setForcedHeight(int userId, Double height) {
        state(userId).forcedHeight = height;
    }

    public static void setForcedRotation(int userId, Integer rotation) {
        state(userId).forcedRotation = rotation;
    }

    /** True while :forcerot is active for this user. */
    public static boolean hasForcedRotation(int userId) {
        return state(userId).forcedRotation != null;
    }

    public static int resolveRotation(int userId, int requestedRotation) {
        Integer forced = state(userId).forcedRotation;
        return forced == null ? requestedRotation : forced;
    }

    public static Double consumeHeight(int userId) {
        State state = state(userId);
        synchronized (state) {
            if (state.batchRemaining > 0) {
                state.batchRemaining--;
                double height = state.batchHeight;
                if (state.batchRemaining == 0) state.batchHeight = 0;
                return height;
            }
            return state.forcedHeight;
        }
    }

    public static void evict(int userId) {
        STATES.remove(userId);
    }

    private static State state(int userId) {
        return STATES.computeIfAbsent(userId, ignored -> new State());
    }

    private static final class State {
        private volatile Double forcedHeight;
        private volatile Integer forcedRotation;
        private int batchRemaining;
        private double batchHeight;

        private synchronized void setBatch(int count, double height) {
            this.batchRemaining = count;
            this.batchHeight = height;
        }
    }
}
