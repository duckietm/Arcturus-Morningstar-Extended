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
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.ItemInteraction;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * The antenna dropdown offers "picked antennas" (0) and "the triggering furni" (1). Picking antennas
 * used to overwrite either choice with the first picked antenna's id, so "triggering furni" could
 * not be kept once an antenna was in the list.
 */
class WiredEffectSendSignalTest {
    private static final int ANTENNA_PICKED = 0;
    private static final int ANTENNA_TRIGGER = 1;

    @Test
    void theTriggeringFurniChoiceSurvivesPickedAntennas() throws Exception {
        Room room = mock(Room.class);
        antenna(room, 401);
        WiredEffectSendSignal box = new WiredEffectSendSignal(1, 1, base(), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            installHotel(emulator, room);

            box.saveData(new WiredSettings(new int[] {ANTENNA_TRIGGER, 0, 0, 0, 0, 0}, "", new int[] {401}, 0), null);
        }

        assertEquals(ANTENNA_TRIGGER, json(box).get("antennaSource").getAsInt());
    }

    @Test
    void thePickedChoiceStillFallsBackToTheFirstPickedAntenna() throws Exception {
        Room room = mock(Room.class);
        antenna(room, 401);
        WiredEffectSendSignal box = new WiredEffectSendSignal(1, 1, base(), "", 0, 0);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            installHotel(emulator, room);

            box.saveData(new WiredSettings(new int[] {ANTENNA_PICKED, 0, 0, 0, 0, 0}, "", new int[] {401}, 0), null);
        }

        assertEquals(401, json(box).get("antennaSource").getAsInt());
    }

    @Test
    void aReloadKeepsTheTriggeringFurniChoiceToo() throws Exception {
        Room room = mock(Room.class);
        antenna(room, 401);
        WiredEffectSendSignal box = new WiredEffectSendSignal(1, 1, base(), "", 0, 0);

        box.loadWiredData(row(stored(ANTENNA_TRIGGER)), room);
        assertEquals(ANTENNA_TRIGGER, json(box).get("antennaSource").getAsInt());

        box.loadWiredData(row(stored(ANTENNA_PICKED)), room);
        assertEquals(401, json(box).get("antennaSource").getAsInt());
    }

    private static String stored(int antennaSource) {
        return "{\"delay\":0,\"itemIds\":[401],\"forwardItemIds\":[],\"antennaSource\":" + antennaSource
                + ",\"furniForward\":0,\"userForward\":0,\"signalPerFurni\":false,\"signalPerUser\":false,"
                + "\"channel\":0}";
    }

    private static void antenna(Room room, int id) {
        ItemInteraction interaction = mock(ItemInteraction.class);
        when(interaction.getName()).thenReturn("antenna");
        Item base = base();
        when(base.getInteractionType()).thenReturn(interaction);
        HabboItem item = mock(HabboItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getBaseItem()).thenReturn(base);
        when(room.getHabboItem(id)).thenReturn(item);
    }
}
