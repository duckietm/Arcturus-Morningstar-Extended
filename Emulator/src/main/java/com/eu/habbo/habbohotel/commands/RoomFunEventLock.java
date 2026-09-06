package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.rooms.Room;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RoomFunEventLock {
    private static final Map<Integer, Long> ACTIVE_EVENTS = new ConcurrentHashMap<>();

    private RoomFunEventLock() {}

    static Long tryAcquire(Room room) {
        long eventId = System.nanoTime();
        return ACTIVE_EVENTS.putIfAbsent(room.getId(), eventId) == null ? eventId : null;
    }

    static boolean isActive(Room room, long eventId) {
        return ACTIVE_EVENTS.getOrDefault(room.getId(), Long.MIN_VALUE) == eventId;
    }

    static void release(Room room, long eventId) {
        ACTIVE_EVENTS.remove(room.getId(), eventId);
    }
}
