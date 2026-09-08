package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.habbo;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserWhisperComposer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** "Sit" has a message field that was saved, stored and sent back to the dialog, and never shown to anyone. */
class WiredEffectSitTest {

    @Test
    void theMessageIsWhisperedToTheUserWhoSat() throws Exception {
        Room room = mock(Room.class);
        RoomUnit unit = mock(RoomUnit.class);
        when(unit.canForcePosture()).thenReturn(true);
        Habbo habbo = habbo("Frank", unit);
        when(room.getHabbo(unit)).thenReturn(habbo);
        WiredEffectSit box = box(room, "Sit down, %user%");

        box.execute(context(room, unit));

        verify(room).makeSit(habbo);
        ArgumentCaptor<RoomUserWhisperComposer> sent = ArgumentCaptor.forClass(RoomUserWhisperComposer.class);
        verify(habbo.getClient()).sendResponse(sent.capture());
        assertEquals("Sit down, Frank", sent.getValue().getRoomChatMessage().getMessage());
    }

    @Test
    void noMessageMeansNoWhisper() throws Exception {
        Room room = mock(Room.class);
        RoomUnit unit = mock(RoomUnit.class);
        when(unit.canForcePosture()).thenReturn(true);
        Habbo habbo = habbo("Frank", unit);
        when(room.getHabbo(unit)).thenReturn(habbo);
        WiredEffectSit box = box(room, "");

        box.execute(context(room, unit));

        verify(room).makeSit(habbo);
        verify(habbo.getClient(), never()).sendResponse(any(MessageComposer.class));
    }

    private static WiredEffectSit box(Room room, String message) throws Exception {
        WiredEffectSit box = new WiredEffectSit(1, 1, base(), "", 0, 0);
        box.loadWiredData(row("{\"message\":\"" + message + "\",\"delay\":0,\"userSource\":0}"), room);
        return box;
    }
}
