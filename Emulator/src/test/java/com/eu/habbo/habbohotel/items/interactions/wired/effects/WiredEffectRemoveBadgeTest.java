package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Remove badge" deletes a badge row from the database for every user it resolves. Taking a badge
 * away is the mirror of granting one, so it sits behind the same reward policy as "give badge".
 */
class WiredEffectRemoveBadgeTest {

    @Test
    void savingWithoutTheRewardPermissionIsRefused() throws Exception {
        WiredEffectRemoveBadge box = new WiredEffectRemoveBadge(1, 1, mock(Item.class), "", 0, 0);
        String before = box.getWiredData();

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            ConfigurationManager config = requiringPermission();
            emulator.when(Emulator::getConfig).thenReturn(config);

            assertFalse(box.saveData(new WiredSettings(new int[] {0}, "ADM", new int[0], 0), null));
        }

        assertEquals(before, box.getWiredData());
    }

    @Test
    void aClientHoldingThePermissionStillSaves() throws Exception {
        WiredEffectRemoveBadge box = new WiredEffectRemoveBadge(1, 1, mock(Item.class), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            ConfigurationManager config = requiringPermission();
            emulator.when(Emulator::getConfig).thenReturn(config);

            assertTrue(box.saveData(new WiredSettings(new int[] {0}, "ADM", new int[0], 0), superWired()));
        }
    }

    private static ConfigurationManager requiringPermission() {
        ConfigurationManager config = mock(ConfigurationManager.class);
        when(config.getBoolean("hotel.wired.reward.require_permission", true)).thenReturn(true);
        return config;
    }

    private static GameClient superWired() {
        Habbo habbo = mock(Habbo.class);
        when(habbo.hasPermission(Permission.ACC_SUPERWIRED)).thenReturn(true);
        GameClient client = mock(GameClient.class);
        when(client.getHabbo()).thenReturn(habbo);
        return client;
    }
}
