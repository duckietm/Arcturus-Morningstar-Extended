package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.messages.outgoing.wired.WiredEffectDataComposer;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The "move user tiles" dialog sends four ints back and saveData reads the tile count from the
 * fourth; the box sent it only three, so every reopen showed one tile whatever had been saved.
 */
class WiredEffectMoveUserTilesTest {

    @Test
    void theDialogGetsTheTileCountAsAFourthInt() throws Exception {
        WiredEffectMoveUserTiles box = new WiredEffectMoveUserTiles(77, 1, base(), "", 0, 0);
        box.loadWiredData(
                row("{\"delay\":0,\"movementDirection\":2,\"rotationDirection\":8,\"userSource\":0,\"tileCount\":7}"),
                null);

        Room room = mock(Room.class);
        RoomSpecialTypes specialTypes = mock(RoomSpecialTypes.class);
        when(specialTypes.getTriggers(anyInt(), anyInt())).thenReturn(Set.of());
        when(room.getRoomSpecialTypes()).thenReturn(specialTypes);

        ByteBuf packet = new WiredEffectDataComposer(box, room).compose().get();
        try {
            packet.skipBytes(6); // frame length and header
            packet.readBoolean();
            packet.readInt();
            packet.readInt();
            packet.readInt();
            packet.readInt();
            packet.skipBytes(packet.readShort()); // the string slot
            assertEquals(4, packet.readInt());
            assertEquals(2, packet.readInt());
            assertEquals(8, packet.readInt());
            assertEquals(0, packet.readInt());
            assertEquals(7, packet.readInt());
        } finally {
            packet.release();
        }
    }
}
