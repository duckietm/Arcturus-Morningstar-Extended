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
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Match furni to snapshot" takes its snapshot from the picked furni. With the dropdown left on
 * "triggering furni" the snapshot was recorded but applied to the trigger item instead.
 */
class WiredEffectMatchFurniTest {

    @Test
    void aSnapshotOfPickedFurniPromotesTheTriggerSourceToSelected() throws Exception {
        Room room = mock(Room.class);
        HabboItem item = mock(HabboItem.class);
        when(item.getId()).thenReturn(301);
        when(item.allowWiredResetState()).thenReturn(true);
        when(item.getExtradata()).thenReturn("1");
        when(room.getHabboItem(301)).thenReturn(item);
        WiredEffectMatchFurni box = new WiredEffectMatchFurni(1, 1, base(), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            installHotel(emulator, room);

            box.saveData(
                    new WiredSettings(new int[] {1, 0, 0, 0, WiredSourceUtil.SOURCE_TRIGGER}, "", new int[] {301}, 0),
                    null);
        }

        assertEquals(
                WiredSourceUtil.SOURCE_SELECTED, json(box).get("furniSource").getAsInt());
        assertEquals(1, json(box).getAsJsonArray("items").size());
    }

    @Test
    void aShortLegacyRowKeepsTheDefaults() throws Exception {
        WiredEffectMatchFurni box = new WiredEffectMatchFurni(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("5"), null);

        assertEquals(
                WiredSourceUtil.SOURCE_TRIGGER, json(box).get("furniSource").getAsInt());
        assertEquals(0, json(box).getAsJsonArray("items").size());
    }
}
