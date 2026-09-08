package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.config;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.installPlatform;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.WiredPlatform;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Picking furni to move with the dropdown left on "triggering furni" means the picked furni, as it
 * does for the sibling boxes and as this box's own loadWiredData already assumed.
 */
class WiredEffectFurniToFurniTest {

    @Test
    void pickedFurniPromoteTheTriggerSourceToSelected() throws Exception {
        Room room = mock(Room.class);
        HabboItem item = mock(HabboItem.class);
        when(item.getId()).thenReturn(301);
        when(room.getHabboItem(301)).thenReturn(item);
        WiredEffectFurniToFurni box = new WiredEffectFurniToFurni(1, 1, base(), "", 0, 0);
        ConfigurationManager config = config();

        try (MockedStatic<WiredPlatform> platform = mockStatic(WiredPlatform.class);
                MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            installPlatform(platform, room);
            emulator.when(Emulator::getConfig).thenReturn(config);

            box.saveData(
                    new WiredSettings(
                            new int[] {WiredSourceUtil.SOURCE_TRIGGER, WiredSourceUtil.SOURCE_TRIGGER},
                            "",
                            new int[] {301},
                            0),
                    null);
        }

        assertEquals(
                WiredSourceUtil.SOURCE_SELECTED, json(box).get("moveSource").getAsInt());
        assertEquals(301, json(box).getAsJsonArray("itemIds").get(0).getAsInt());
    }
}
