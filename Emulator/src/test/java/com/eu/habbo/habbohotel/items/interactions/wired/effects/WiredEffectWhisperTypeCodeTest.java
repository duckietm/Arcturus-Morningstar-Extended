package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.messages.outgoing.wired.WiredEffectDataComposer;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The show-message serializer wrote its own static type code, so the boxes built on it that answer
 * a different type - give effect, give hand item, alert - told the client to open the show-message
 * dialog. The negative show-message box shares the code and is unaffected either way.
 */
class WiredEffectWhisperTypeCodeTest {

    @Test
    void giveEffectAdvertisesItsOwnDialog() {
        assertEquals(WiredEffectType.EFFECT_ID.code, advertisedCode(new WiredEffectGiveEffect(1, 1, base(), "", 0, 0)));
    }

    @Test
    void giveHandItemAdvertisesItsOwnDialog() {
        assertEquals(
                WiredEffectType.EFFECT_ID.code, advertisedCode(new WiredEffectGiveHandItem(1, 1, base(), "", 0, 0)));
    }

    @Test
    void alertAdvertisesItsOwnDialog() {
        assertEquals(WiredEffectType.EFFECT_MESSAGE.code, advertisedCode(new WiredEffectAlert(1, 1, base(), "", 0, 0)));
    }

    @Test
    void theShowMessageBoxesStillAdvertiseTheShowMessageDialog() {
        assertEquals(WiredEffectType.SHOW_MESSAGE.code, advertisedCode(new WiredEffectWhisper(1, 1, base(), "", 0, 0)));
        assertEquals(
                WiredEffectType.SHOW_MESSAGE.code,
                advertisedCode(new WiredEffectNegativeShowMessage(1, 1, base(), "", 0, 0)));
    }

    /** Reads the type code out of the packet the dialog receives, past the four ints the box sends. */
    private static int advertisedCode(InteractionWiredEffect effect) {
        Room room = mock(Room.class);
        RoomSpecialTypes specialTypes = mock(RoomSpecialTypes.class);
        when(specialTypes.getTriggers(anyInt(), anyInt())).thenReturn(Set.of());
        when(room.getRoomSpecialTypes()).thenReturn(specialTypes);

        ByteBuf packet = new WiredEffectDataComposer(effect, room).compose().get();
        try {
            packet.skipBytes(6); // frame length and header
            packet.readBoolean();
            packet.readInt();
            packet.readInt();
            packet.readInt();
            packet.readInt();
            packet.skipBytes(packet.readShort()); // the message
            int count = packet.readInt();
            for (int i = 0; i < count; i++) {
                packet.readInt();
            }
            packet.readInt(); // the selection code slot
            return packet.readInt();
        } finally {
            packet.release();
        }
    }
}
