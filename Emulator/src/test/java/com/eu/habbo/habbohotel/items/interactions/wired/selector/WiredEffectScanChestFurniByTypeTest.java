package com.eu.habbo.habbohotel.items.interactions.wired.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.chest.ChestStorage;
import com.eu.habbo.habbohotel.items.interactions.wired.chest.InteractionWiredChest;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredServices;
import com.eu.habbo.habbohotel.wired.core.WiredState;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A chest decides whether wired may look inside it. The chest effects and Init Transaction honour
 * that; the scanner is read-only but it still discloses what the chest holds, so it must ask too.
 */
class WiredEffectScanChestFurniByTypeTest {

    @Test
    void aChestThatDoesNotAnswerWiredIsNotRead() throws Exception {
        Room room = mock(Room.class);
        HabboItem matching = furni(777);
        when(room.getFloorItems()).thenReturn(Set.of(matching));
        InteractionWiredChest silentChest = chest(false, 777);
        when(room.getHabboItem(501)).thenReturn(silentChest);

        WiredEffectScanChestFurniByType box = configured();
        WiredContext ctx = context(room);
        box.execute(ctx);

        assertTrue(ctx.targets().items().isEmpty());
    }

    @Test
    void aChestThatAnswersWiredIsScanned() throws Exception {
        Room room = mock(Room.class);
        HabboItem matching = furni(777);
        when(room.getFloorItems()).thenReturn(Set.of(matching));
        InteractionWiredChest openChest = chest(true, 777);
        when(room.getHabboItem(501)).thenReturn(openChest);

        WiredEffectScanChestFurniByType box = configured();
        WiredContext ctx = context(room);
        box.execute(ctx);

        assertEquals(Set.of(matching), ctx.targets().items());
    }

    private static WiredEffectScanChestFurniByType configured() throws Exception {
        WiredEffectScanChestFurniByType box = new WiredEffectScanChestFurniByType(1, 1, mock(Item.class), "", 0, 0);
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data"))
                .thenReturn("{\"filterExisting\":false,\"invert\":false,\"chestIds\":[501],\"delay\":0}");
        box.loadWiredData(set, null);
        return box;
    }

    private static InteractionWiredChest chest(boolean answersWired, int storedBaseItemId) {
        InteractionWiredChest chest = mock(InteractionWiredChest.class);
        ChestStorage contents = mock(ChestStorage.class);
        when(contents.distinctTypes(ChestStorage.KIND_FURNI)).thenReturn(List.of(storedBaseItemId));
        when(chest.answersWired()).thenReturn(answersWired);
        when(chest.getContents()).thenReturn(contents);
        return chest;
    }

    private static HabboItem furni(int baseItemId) {
        HabboItem item = mock(HabboItem.class);
        Item base = mock(Item.class);
        when(base.getId()).thenReturn(baseItemId);
        when(item.getBaseItem()).thenReturn(base);
        return item;
    }

    private static WiredContext context(Room room) {
        return new WiredContext(
                WiredEvent.builder(WiredEvent.Type.CUSTOM, room).build(),
                null,
                mock(WiredServices.class),
                new WiredState(20));
    }
}
