package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.boxBase;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.room;
import static com.eu.habbo.habbohotel.items.interactions.wired.conditions.WiredConditionTestSupport.settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.habbohotel.wired.core.WiredServices;
import com.eu.habbo.habbohotel.wired.core.WiredState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "A furni next to the trigger has state X": the state is stored the way its sibling "user on furni
 * with state" stores it, only furni the room can resolve are kept, and a furni without a base item
 * is skipped rather than dereferenced.
 */
class WiredConditionTriggerFurniAdjacentStateTest {

    private static final String SELECTION_CAP = "hotel.wired.furni.selection.count";

    @Test
    void theRequiredStateIsTrimmedAndCappedOnLoad() throws Exception {
        WiredConditionTriggerFurniAdjacentState box = box();

        box.loadWiredData(row("{\"state\":\"  on  \",\"furni\":[]}"), null);
        assertEquals("on", stored(box).get("state").getAsString());

        box.loadWiredData(row("{\"state\":\"" + "x".repeat(300) + "\",\"furni\":[]}"), null);
        assertEquals(256, stored(box).get("state").getAsString().length());
    }

    @Test
    void savingTrimsTheStateAndKeepsOnlyFurniTheRoomKnows() {
        WiredConditionTriggerFurniAdjacentState box = box();
        Room room = room(1);
        HabboItem known = mock(HabboItem.class);
        when(known.getId()).thenReturn(7);
        when(room.getHabboItem(7)).thenReturn(known);

        WiredSettings settings = settings(new int[0], 7, 8);
        settings.setStringParam("  on  ");

        GameEnvironment environment = environmentWith(room);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            ConfigurationManager config = mock(ConfigurationManager.class);
            when(config.getInt(SELECTION_CAP, WiredManager.MAXIMUM_FURNI_SELECTION))
                    .thenReturn(5);
            emulator.when(Emulator::getConfig).thenReturn(config);
            emulator.when(Emulator::getGameEnvironment).thenReturn(environment);

            assertTrue(box.saveData(settings));
        }

        JsonObject saved = stored(box);
        assertEquals("on", saved.get("state").getAsString());
        JsonArray furni = saved.get("furni").getAsJsonArray();
        assertEquals(1, furni.size(), "the id the room cannot resolve is not a selection");
        assertEquals(7, furni.get(0).getAsInt());
    }

    @Test
    void aFurniWithoutABaseItemIsSkippedInsteadOfCrashing() throws Exception {
        Room room = room(1);
        RoomLayout layout = mock(RoomLayout.class);
        RoomTile here = mock(RoomTile.class);
        RoomTile next = mock(RoomTile.class);
        when(room.getLayout()).thenReturn(layout);
        when(layout.getTile((short) 0, (short) 0)).thenReturn(here);
        when(layout.getTilesAround(here)).thenReturn(List.of(next));

        HabboItem trigger = mock(HabboItem.class);
        when(trigger.getId()).thenReturn(99);

        // Neither the selected furni nor the neighbour has a base item to read a type from.
        HabboItem headless = mock(HabboItem.class);
        when(headless.getId()).thenReturn(5);
        when(headless.getExtradata()).thenReturn("on");
        when(room.getHabboItem(5)).thenReturn(headless);
        when(room.getItemsAt(next)).thenReturn(Set.of(headless));

        WiredConditionTriggerFurniAdjacentState box = box();
        box.loadWiredData(row("{\"state\":\"on\",\"furni\":[5]}"), room);

        WiredContext ctx = new WiredContext(
                WiredEvent.builder(WiredEvent.Type.CUSTOM, room).build(),
                trigger,
                mock(WiredServices.class),
                new WiredState(20));

        assertTrue(box.evaluate(ctx), "with no type to scope by, the neighbour's state alone decides");
    }

    private static WiredConditionTriggerFurniAdjacentState box() {
        return new WiredConditionTriggerFurniAdjacentState(1, 1, boxBase(), "", 0, 0);
    }

    private static JsonObject stored(WiredConditionTriggerFurniAdjacentState box) {
        return JsonParser.parseString(box.getWiredData()).getAsJsonObject();
    }

    private static ResultSet row(String wiredData) throws SQLException {
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data")).thenReturn(wiredData);
        return set;
    }

    /** A game environment whose room manager answers this room for every id. */
    private static GameEnvironment environmentWith(Room room) {
        RoomManager rooms = mock(RoomManager.class);
        when(rooms.getRoom(anyInt())).thenReturn(room);
        GameEnvironment environment = mock(GameEnvironment.class);
        when(environment.getRoomManager()).thenReturn(rooms);
        return environment;
    }
}
