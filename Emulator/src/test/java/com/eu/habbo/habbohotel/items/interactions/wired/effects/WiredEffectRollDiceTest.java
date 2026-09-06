package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDice;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredServices;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.habbohotel.wired.core.WiredState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** "Roll dice": every selected dice rolls, as if a user next to it had clicked; anything else is left alone. */
class WiredEffectRollDiceTest {

    @Test
    void rollsTheSelectedDiceAndNothingElse() throws Exception {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(1);
        HabboItem dice = dice(room, 301);
        HabboItem otherDice = dice(room, 302);
        HabboItem lamp = furni(room, 303);
        List<Integer> rolled = new ArrayList<>();

        WiredEffectRollDice box = new WiredEffectRollDice(1, 1, base(), "", 0, 0);
        box.roller((item, r) -> rolled.add(item.getId()));
        box.saveData(
                new WiredSettings(new int[] {WiredSourceUtil.SOURCE_SELECTED}, "", new int[] {301, 302, 303}, 0), null);

        box.execute(context(room));

        assertEquals(List.of(301, 302), rolled);
    }

    private static WiredContext context(Room room) {
        return new WiredContext(
                WiredEvent.builder(WiredEvent.Type.CUSTOM, room).build(),
                null,
                mock(WiredServices.class),
                new WiredState(20));
    }

    private static Item base() {
        Item base = mock(Item.class);
        when(base.getType()).thenReturn(FurnitureType.FLOOR);
        when(base.getSpriteId()).thenReturn(4321);
        return base;
    }

    private static HabboItem dice(Room room, int id) {
        InteractionDice item = mock(InteractionDice.class);
        Item base = base();
        when(item.getId()).thenReturn(id);
        when(item.getBaseItem()).thenReturn(base);
        when(room.getHabboItem(id)).thenReturn(item);
        return item;
    }

    private static HabboItem furni(Room room, int id) {
        HabboItem item = mock(HabboItem.class);
        Item base = base();
        when(item.getId()).thenReturn(id);
        when(item.getBaseItem()).thenReturn(base);
        when(room.getHabboItem(id)).thenReturn(item);
        return item;
    }
}
