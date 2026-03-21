package com.eu.habbo.habbohotel.gameclients;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages a grace period for disconnected users. Instead of immediately
 * disposing a Habbo when their WebSocket drops, the Habbo is held in
 * a "ghost" state for a configurable number of seconds. If the same
 * user reconnects (via SSO ticket) within the grace window, their
 * existing Habbo object is resumed on the new connection — keeping
 * them in their room, preserving inventory state, etc.
 *
 * Config key: session.reconnect.grace.seconds (default: 30)
 */
public class SessionResumeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionResumeManager.class);

    private static SessionResumeManager instance;

    private final ConcurrentHashMap<Integer, GhostSession> ghostSessions = new ConcurrentHashMap<>();

    public static SessionResumeManager getInstance() {
        if (instance == null) {
            instance = new SessionResumeManager();
        }
        return instance;
    }

    public int getGracePeriodSeconds() {
        return Emulator.getConfig().getInt("session.reconnect.grace.seconds", 30);
    }

    /**
     * Park a disconnected Habbo in ghost mode. Their room presence is
     * preserved, but the old GameClient channel is closed.
     *
     * @return true if the habbo was parked (grace period > 0), false if immediate dispose should happen
     */
    public boolean parkHabbo(Habbo habbo, String ssoTicket) {
        int graceSeconds = getGracePeriodSeconds();
        if (graceSeconds <= 0) {
            return false;
        }

        int userId = habbo.getHabboInfo().getId();

        // Cancel any existing ghost session for this user
        GhostSession existing = ghostSessions.remove(userId);
        if (existing != null && existing.disposeFuture != null) {
            existing.disposeFuture.cancel(false);
        }

        LOGGER.info("[SessionResume] Parking {} (id={}) for {}s grace period",
                habbo.getHabboInfo().getUsername(), userId, graceSeconds);

        // Restore the SSO ticket so the client can reconnect with the same ticket
        if (ssoTicket != null && !ssoTicket.isEmpty()) {
            restoreSsoTicket(userId, ssoTicket);
        }

        // Schedule the final disconnect after the grace period
        ScheduledFuture<?> future = Emulator.getThreading().run(() -> {
            GhostSession ghost = ghostSessions.remove(userId);
            if (ghost != null) {
                LOGGER.info("[SessionResume] Grace period expired for {} (id={}) - performing full disconnect",
                        ghost.habbo.getHabboInfo().getUsername(), userId);
                performFullDisconnect(ghost.habbo);
            }
        }, graceSeconds * 1000);

        ghostSessions.put(userId, new GhostSession(habbo, ssoTicket, future));
        return true;
    }

    /**
     * Try to resume a ghost session for the given user ID.
     *
     * @return the parked Habbo if found within grace period, null otherwise
     */
    public Habbo resumeSession(int userId) {
        GhostSession ghost = ghostSessions.remove(userId);
        if (ghost == null) {
            return null;
        }

        // Cancel the scheduled dispose
        if (ghost.disposeFuture != null) {
            ghost.disposeFuture.cancel(false);
        }

        LOGGER.info("[SessionResume] Resuming session for {} (id={})",
                ghost.habbo.getHabboInfo().getUsername(), userId);

        return ghost.habbo;
    }

    /**
     * Check if a user has a ghost session (is in grace period).
     */
    public boolean hasGhostSession(int userId) {
        return ghostSessions.containsKey(userId);
    }

    /**
     * Immediately expire all ghost sessions (e.g. on emulator shutdown).
     */
    public void disposeAll() {
        for (GhostSession ghost : ghostSessions.values()) {
            if (ghost.disposeFuture != null) {
                ghost.disposeFuture.cancel(false);
            }
            performFullDisconnect(ghost.habbo);
        }
        ghostSessions.clear();
    }

    /**
     * Perform the actual full disconnect that normally happens in Habbo.disconnect().
     */
    private void performFullDisconnect(Habbo habbo) {
        try {
            habbo.getHabboInfo().setOnline(false);
            habbo.disconnect();
        } catch (Exception e) {
            LOGGER.error("[SessionResume] Error during deferred disconnect", e);
        }

        // Clear the SSO ticket now that the grace period is truly over
        clearSsoTicket(habbo.getHabboInfo().getId());
    }

    private void restoreSsoTicket(int userId, String ssoTicket) {
        try (var connection = Emulator.getDatabase().getDataSource().getConnection();
             var statement = connection.prepareStatement("UPDATE users SET auth_ticket = ? WHERE id = ? LIMIT 1")) {
            statement.setString(1, ssoTicket);
            statement.setInt(2, userId);
            statement.execute();
            LOGGER.info("[SessionResume] Restored SSO ticket for user {} during grace period", userId);
        } catch (Exception e) {
            LOGGER.error("[SessionResume] Failed to restore SSO ticket for user " + userId, e);
        }
    }

    private void clearSsoTicket(int userId) {
        try (var connection = Emulator.getDatabase().getDataSource().getConnection();
             var statement = connection.prepareStatement("UPDATE users SET auth_ticket = ? WHERE id = ? LIMIT 1")) {
            statement.setString(1, "");
            statement.setInt(2, userId);
            statement.execute();
        } catch (Exception e) {
            LOGGER.error("[SessionResume] Failed to clear SSO ticket for user " + userId, e);
        }
    }

    private static class GhostSession {
        final Habbo habbo;
        final String ssoTicket;
        final ScheduledFuture<?> disposeFuture;

        GhostSession(Habbo habbo, String ssoTicket, ScheduledFuture<?> disposeFuture) {
            this.habbo = habbo;
            this.ssoTicket = ssoTicket;
            this.disposeFuture = disposeFuture;
        }
    }
}
