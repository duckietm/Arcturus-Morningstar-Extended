package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.json;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import org.junit.jupiter.api.Test;

/**
 * "Mute user" clamps its length on save and on execute but not on load, used the user's client
 * without checking it was still there, and could carry a null message after a short legacy row.
 */
class WiredEffectMuteHabboTest {

    @Test
    void aStoredLengthPastTheCapLoadsClamped() throws Exception {
        WiredEffectMuteHabbo box = new WiredEffectMuteHabbo(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("{\"delay\":0,\"length\":99999,\"userSource\":0}"), null);

        assertEquals(24 * 60, json(box).get("length").getAsInt());
    }

    @Test
    void aRowWithoutAMessageLoadsAnEmptyOne() throws Exception {
        WiredEffectMuteHabbo box = new WiredEffectMuteHabbo(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("{\"delay\":0,\"length\":5,\"userSource\":0}"), null);

        assertEquals("", json(box).get("message").getAsString());
    }

    @Test
    void aUserWhoseClientIsGoneIsStillMutedAndNothingFails() throws Exception {
        Room room = mock(Room.class);
        RoomUnit unit = mock(RoomUnit.class);
        Habbo habbo = mock(Habbo.class);
        when(habbo.getHabboInfo()).thenReturn(mock(HabboInfo.class));
        when(room.getHabbo(unit)).thenReturn(habbo);
        WiredEffectMuteHabbo box = new WiredEffectMuteHabbo(1, 1, base(), "", 0, 0);
        box.loadWiredData(row("{\"delay\":0,\"length\":5,\"message\":\"Quiet\",\"userSource\":0}"), room);

        box.execute(context(room, unit));

        verify(room).muteHabbo(habbo, 5);
    }
}
