package com.eu.habbo.habbohotel.items.interactions.wired.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

/**
 * The neighborhood selectors read their source from an int the dialog sends and from the saved
 * row. A value outside the six sources used to be stored as-is, and a box carrying one selected
 * nothing without saying why. Out-of-range values now fall back to the trigger user, in both
 * directions.
 */
class WiredEffectNeighborhoodSourceTypeTest {

    @Test
    void furniNeighborhoodClampsTheSourceOnSaveAndLoad() throws Exception {
        WiredEffectFurniNeighborhood box = new WiredEffectFurniNeighborhood(1, 1, base(), "", 0, 0);

        box.saveData(new WiredSettings(new int[] {99}, "", new int[0], 0), null);
        assertEquals(0, furniSourceOf(box));

        box.saveData(new WiredSettings(new int[] {5}, "", new int[0], 0), null);
        assertEquals(5, furniSourceOf(box));

        box.loadWiredData(row("{\"sourceType\":-3,\"tileOffsets\":[],\"pickedFurniIds\":[],\"delay\":0}"), null);
        assertEquals(0, furniSourceOf(box));
    }

    @Test
    void usersNeighborhoodClampsTheSourceOnSaveAndLoad() throws Exception {
        WiredEffectUsersNeighborhood box = new WiredEffectUsersNeighborhood(1, 1, base(), "", 0, 0);

        box.saveData(new WiredSettings(new int[] {99}, "", new int[0], 0), null);
        assertEquals(0, usersSourceOf(box));

        box.saveData(new WiredSettings(new int[] {5}, "", new int[0], 0), null);
        assertEquals(5, usersSourceOf(box));

        box.loadWiredData(row("{\"sourceType\":-3,\"tileOffsets\":[],\"pickedFurniIds\":[],\"delay\":0}"), null);
        assertEquals(0, usersSourceOf(box));
    }

    private static int furniSourceOf(WiredEffectFurniNeighborhood box) {
        return WiredManager.getGson()
                .fromJson(box.getWiredData(), WiredEffectFurniNeighborhood.JsonData.class)
                .sourceType;
    }

    private static int usersSourceOf(WiredEffectUsersNeighborhood box) {
        return WiredManager.getGson()
                .fromJson(box.getWiredData(), WiredEffectUsersNeighborhood.JsonData.class)
                .sourceType;
    }

    private static ResultSet row(String wiredData) throws Exception {
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data")).thenReturn(wiredData);
        return set;
    }

    private static Item base() {
        return mock(Item.class);
    }
}
