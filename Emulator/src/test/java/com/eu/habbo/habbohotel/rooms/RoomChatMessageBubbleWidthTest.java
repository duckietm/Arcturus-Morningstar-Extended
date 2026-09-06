package com.eu.habbo.habbohotel.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

/**
 * A wired message may ask for a bubble width of its own. The client reads it as an optional int at
 * the very end of the chat packet, so a message without one must end exactly where it ended before
 * plus a -1, and old clients that stop reading earlier lose nothing.
 */
class RoomChatMessageBubbleWidthTest {

    @Test
    void aPlainMessageEndsWithNoOverride() {
        assertEquals(-1, trailingInt(message()));
    }

    @Test
    void theOverrideTravelsAtTheEndOfThePacket() {
        RoomChatMessage message = message();
        message.setBubbleWidthOverride(2);

        assertEquals(2, trailingInt(message));
    }

    @Test
    void aWidthThatNamesNothingIsNoOverride() {
        RoomChatMessage message = message();
        message.setBubbleWidthOverride(7);

        assertEquals(-1, message.getBubbleWidthOverride());
    }

    @Test
    void theCopyKeepsTheOverride() {
        RoomChatMessage message = message();
        message.setBubbleWidthOverride(0);

        assertEquals(0, new RoomChatMessage(message).getBubbleWidthOverride());
    }

    private static RoomChatMessage message() {
        RoomUnit unit = mock(RoomUnit.class);
        when(unit.getId()).thenReturn(5);
        return new RoomChatMessage("hello", unit, RoomChatMessageBubbles.NORMAL);
    }

    private static int trailingInt(RoomChatMessage message) {
        ByteBuf packet = new RoomUserTalkComposer(message).compose().get();
        try {
            return packet.getInt(packet.writerIndex() - 4);
        } finally {
            packet.release();
        }
    }
}
