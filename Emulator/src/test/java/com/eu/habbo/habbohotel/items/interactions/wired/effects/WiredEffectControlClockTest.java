package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.installHotel;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.json;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Control clock" validated the picked clocks and stored them, then with the dropdown left on
 * "triggering furni" resolved the trigger item at run time and controlled nothing.
 */
class WiredEffectControlClockTest {

    @Test
    void pickedClocksPromoteTheTriggerSourceToSelected() throws Exception {
        Room room = mock(Room.class);
        InteractionGameTimer clock = mock(InteractionGameTimer.class);
        when(clock.getId()).thenReturn(601);
        when(room.getHabboItem(601)).thenReturn(clock);
        WiredEffectControlClock box = new WiredEffectControlClock(1, 1, base(), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            installHotel(emulator, room);

            box.saveData(
                    new WiredSettings(new int[] {0, WiredSourceUtil.SOURCE_TRIGGER}, "", new int[] {601}, 0), null);
        }

        assertEquals(
                WiredSourceUtil.SOURCE_SELECTED, json(box).get("furniSource").getAsInt());
        assertEquals(601, json(box).getAsJsonArray("itemIds").get(0).getAsInt());
    }

    @Test
    void aStoredSourceThatNamesNothingLoadsAsTheTrigger() throws Exception {
        WiredEffectControlClock box = new WiredEffectControlClock(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("{\"delay\":0,\"itemIds\":[],\"action\":0,\"furniSource\":999}"), mock(Room.class));

        assertEquals(
                WiredSourceUtil.SOURCE_TRIGGER, json(box).get("furniSource").getAsInt());
    }
}
