package com.eu.habbo.networking.gameserver.auth;

import com.eu.habbo.Emulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class AuthRateLimiter {

    private static final Map<String, AtomicReference<State>> STATE = new ConcurrentHashMap<>();

    private AuthRateLimiter() {}

    public static boolean isLocked(String ip) {
        if (!isEnabled() || ip == null || ip.isEmpty()) return false;

        AtomicReference<State> ref = STATE.get(ip);
        if (ref == null) return false;

        State current = ref.get();
        return current != null && current.lockedUntilMillis > System.currentTimeMillis();
    }

    public static long secondsUntilUnlock(String ip) {
        AtomicReference<State> ref = STATE.get(ip);
        if (ref == null) return 0;

        State current = ref.get();
        if (current == null) return 0;

        long remainingMs = current.lockedUntilMillis - System.currentTimeMillis();
        return remainingMs > 0 ? (remainingMs / 1000L) + 1L : 0L;
    }

    public static void recordFailure(String ip) {
        if (!isEnabled() || ip == null || ip.isEmpty()) return;

        long now = System.currentTimeMillis();
        long windowMs = configInt("login.ratelimit.window_sec", 60) * 1000L;
        int maxAttempts = configInt("login.ratelimit.max_attempts", 5);
        long lockoutMs = configInt("login.ratelimit.lockout_sec", 120) * 1000L;

        STATE.computeIfAbsent(ip, k -> new AtomicReference<>(new State(0, 0L, 0L)))
                .updateAndGet(prev -> {
                    if (prev == null || (now - prev.windowStartMillis) > windowMs) {
                        return new State(1, now, 0L);
                    }

                    int attempts = prev.attempts + 1;
                    long lockedUntil = attempts >= maxAttempts ? now + lockoutMs : 0L;
                    return new State(attempts, prev.windowStartMillis, lockedUntil);
                });
    }

    public static void recordSuccess(String ip) {
        if (ip == null || ip.isEmpty()) return;
        STATE.remove(ip);
    }

    private static boolean isEnabled() {
        return Emulator.getConfig() != null
                && Emulator.getConfig().getBoolean("login.ratelimit.enabled", true);
    }

    private static int configInt(String key, int fallback) {
        return Emulator.getConfig() != null ? Emulator.getConfig().getInt(key, fallback) : fallback;
    }

    private record State(int attempts, long windowStartMillis, long lockedUntilMillis) {}
}
