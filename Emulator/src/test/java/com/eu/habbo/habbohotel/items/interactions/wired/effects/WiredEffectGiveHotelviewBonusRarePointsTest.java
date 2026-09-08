package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredServices;
import com.eu.habbo.habbohotel.wired.core.WiredState;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

/**
 * The points type comes from configuration. saveData already copes with the configuration not
 * being there; execute must too, and fall back to the type the hotel ships with, rather than
 * failing the whole stack on the first firing.
 */
class WiredEffectGiveHotelviewBonusRarePointsTest {

    @Test
    void firesWithTheDefaultPointsTypeWhenNoConfigurationIsInstalled() throws Exception {
        Room room = mock(Room.class);
        RoomUnit unit = mock(RoomUnit.class);
        Habbo standingThere = mock(Habbo.class);
        when(room.getHabbo(unit)).thenReturn(standingThere);

        WiredEffectGiveHotelviewBonusRarePoints box =
                new WiredEffectGiveHotelviewBonusRarePoints(1, 1, mock(Item.class), "", 0, 0);
        ResultSet set = mock(ResultSet.class);
        when(set.getString("wired_data")).thenReturn("{\"delay\":0,\"amount\":3,\"userSource\":0}");
        box.loadWiredData(set, room);

        // Nothing installs the platform here, so configuration() answers null: the same situation
        // as a hotel whose config row is missing.
        box.execute(new WiredContext(
                WiredEvent.builder(WiredEvent.Type.CUSTOM, room).actor(unit).build(),
                null,
                mock(WiredServices.class),
                new WiredState(20)));

        verify(standingThere).givePoints(5, 3);
    }
}
