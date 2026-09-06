package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.ItemManager;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Place furni" mints a real, persisted furni owned by the room owner on every firing. It hands
 * out value the same way "give furni" does, so it is gated by the same reward policy.
 */
class WiredEffectPlaceFurniTest {

    private static final int[] PARAMS = {1389, 1, 0, 0, 0, 0};

    @Test
    void savingWithoutTheRewardPermissionIsRefusedBeforeAnythingElseIsLookedAt() throws Exception {
        WiredEffectPlaceFurni box = new WiredEffectPlaceFurni(1, 1, mock(Item.class), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            ConfigurationManager gate = requiringPermission();
            emulator.when(Emulator::getConfig).thenReturn(gate);
            // No game environment is wired up: were the gate not the first thing saveData did,
            // the item lookup would fail here instead of the policy answering.

            assertFalse(box.saveData(new WiredSettings(PARAMS, "", new int[0], 0), null));
        }
    }

    @Test
    void aClientHoldingThePermissionStillSaves() throws Exception {
        WiredEffectPlaceFurni box = new WiredEffectPlaceFurni(1, 1, mock(Item.class), "", 0, 0);
        Item floor = mock(Item.class);
        when(floor.getType()).thenReturn(FurnitureType.FLOOR);
        ItemManager items = mock(ItemManager.class);
        when(items.getItem(1389)).thenReturn(floor);
        GameEnvironment environment = mock(GameEnvironment.class);
        when(environment.getItemManager()).thenReturn(items);
        ConfigurationManager config = requiringPermission();
        when(config.getInt("hotel.wired.max_delay", 20)).thenReturn(20);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            emulator.when(Emulator::getConfig).thenReturn(config);
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);

            assertTrue(box.saveData(new WiredSettings(PARAMS, "", new int[0], 0), superWired()));
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
