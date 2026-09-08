package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.boxBase;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.room;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

/**
 * "First trigger" and "daily trigger": a user passes once, ever, or once per day. Who has already
 * passed is written with the box, so a server restart does not hand out a second first time.
 */
class WiredConditionUserOnceTest {

    @Test
    void firstTimePassesOncePerUserForever() {
        Room room = room(1);
        RoomUnit actor = user(room, 10, 1);
        RoomUnit other = user(room, 11, 1);
        WiredConditionUserFirstTime box = new WiredConditionUserFirstTime(1, 1, boxBase(), "", 0, 0);

        assertTrue(box.evaluate(context(room, actor)));
        assertFalse(box.evaluate(context(room, actor)));
        assertTrue(box.evaluate(context(room, other)));
        assertFalse(box.evaluate(context(room, null)));
    }

    @Test
    void firstTimeRemembersAcrossAReload() throws Exception {
        Room room = room(1);
        RoomUnit actor = user(room, 10, 1);
        WiredConditionUserFirstTime box = new WiredConditionUserFirstTime(1, 1, boxBase(), "", 0, 0);
        assertTrue(box.evaluate(context(room, actor)));

        ResultSet stored = mock(ResultSet.class);
        when(stored.getString("wired_data")).thenReturn(box.getWiredData());
        WiredConditionUserFirstTime reloaded = new WiredConditionUserFirstTime(1, 1, boxBase(), "", 0, 0);
        reloaded.loadWiredData(stored, room);

        assertFalse(reloaded.evaluate(context(room, actor)));
        assertTrue(reloaded.evaluate(context(room, user(room, 12, 1))));
    }

    @Test
    void pickingTheBoxUpForgetsEveryone() {
        Room room = room(1);
        RoomUnit actor = user(room, 10, 1);
        WiredConditionUserFirstTime box = new WiredConditionUserFirstTime(1, 1, boxBase(), "", 0, 0);
        assertTrue(box.evaluate(context(room, actor)));

        box.onPickUp();

        assertTrue(box.evaluate(context(room, actor)));
    }

    @Test
    void dailyPassesOncePerUserPerDay() {
        Room room = room(1);
        RoomUnit actor = user(room, 10, 1);
        long[] day = {20_000L};
        WiredConditionUserDaily box = new WiredConditionUserDaily(1, 1, boxBase(), "", 0, 0);
        box.dayClock(() -> day[0]);

        assertTrue(box.evaluate(context(room, actor)));
        assertFalse(box.evaluate(context(room, actor)));
        day[0]++;
        assertTrue(box.evaluate(context(room, actor)));
        assertFalse(box.evaluate(context(room, actor)));
    }

    @Test
    void dailyRemembersAcrossAReload() throws Exception {
        Room room = room(1);
        RoomUnit actor = user(room, 10, 1);
        WiredConditionUserDaily box = new WiredConditionUserDaily(1, 1, boxBase(), "", 0, 0);
        box.dayClock(() -> 20_000L);
        assertTrue(box.evaluate(context(room, actor)));

        ResultSet stored = mock(ResultSet.class);
        when(stored.getString("wired_data")).thenReturn(box.getWiredData());
        WiredConditionUserDaily reloaded = new WiredConditionUserDaily(1, 1, boxBase(), "", 0, 0);
        reloaded.dayClock(() -> 20_000L);
        reloaded.loadWiredData(stored, room);

        assertFalse(reloaded.evaluate(context(room, actor)));
    }
}
