package com.eu.habbo.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.rooms.RoomChatManager;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboStats;
import com.eu.habbo.habbohotel.users.UserWordFilter;
import com.eu.habbo.messages.incoming.Incoming;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;
import com.eu.habbo.messages.outgoing.users.CustomWordFilterModifyResultComposer;
import com.eu.habbo.messages.outgoing.users.CustomWordFilterWordsComposer;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Wire contract of the AIR 13 personal word filter (class_1956 header table). */
class CustomWordFilterPacketContractTest {

    @Test
    void headersMatchTheOfficialClient() {
        assertEquals(145, Incoming.RequestCustomWordFilterEvent);
        assertEquals(68, Incoming.AddCustomWordFilterWordEvent);
        assertEquals(1996, Incoming.RemoveCustomWordFilterWordEvent);
        assertEquals(3883, Outgoing.CustomWordFilterWordsComposer);
        assertEquals(3333, Outgoing.CustomWordFilterModifyResultComposer);
    }

    @Test
    void wordListIsACountedStringBlock() {
        ByteBuf packet = new CustomWordFilterWordsComposer(List.of("pippo", "pluto"))
                .compose()
                .get();
        packet.skipBytes(4);

        assertEquals(3883, packet.readShort());
        assertEquals(2, packet.readInt());
        assertEquals("pippo", readString(packet));
        assertEquals("pluto", readString(packet));
        assertFalse(packet.isReadable());
    }

    @Test
    void modifyResultCarriesTheResultCodeThenTheWord() {
        ByteBuf packet = new CustomWordFilterModifyResultComposer(CustomWordFilterModifyResultComposer.REMOVED, "pippo")
                .compose()
                .get();
        packet.skipBytes(4);

        assertEquals(3333, packet.readShort());
        assertEquals(3, packet.readInt());
        assertEquals("pippo", readString(packet));
        assertFalse(packet.isReadable());
    }

    @Test
    void recipientsWithAMatchingPersonalFilterGetTheirOwnMaskedPacket() {
        RoomChatMessage chat = message("ciao Pippo");
        ServerMessage shared = new RoomUserTalkComposer(chat).compose();
        Habbo speaker = habboWithFilter();
        Habbo listener = habboWithFilter("pippo");
        Habbo bystander = habboWithFilter("pluto");

        assertSame(shared, RoomChatManager.chatPacketFor(speaker, speaker, chat, shared, RoomUserTalkComposer::new));
        assertSame(shared, RoomChatManager.chatPacketFor(bystander, speaker, chat, shared, RoomUserTalkComposer::new));

        ServerMessage personal =
                RoomChatManager.chatPacketFor(listener, speaker, chat, shared, RoomUserTalkComposer::new);
        assertNotSame(shared, personal);
        ByteBuf packet = personal.get();
        packet.skipBytes(6);
        assertEquals(5, packet.readInt());
        assertEquals("ciao bobba", readString(packet));
        // the shared packet and the original message are untouched
        assertEquals("ciao Pippo", chat.getMessage());
    }

    private static Habbo habboWithFilter(String... words) {
        UserWordFilter filter = new UserWordFilter();
        for (String word : words) {
            filter.add(word);
        }
        HabboStats stats = mock(HabboStats.class);
        when(stats.getCustomWordFilter()).thenReturn(filter);
        Habbo habbo = mock(Habbo.class);
        when(habbo.getHabboStats()).thenReturn(stats);
        return habbo;
    }

    private static RoomChatMessage message(String text) {
        RoomUnit unit = mock(RoomUnit.class);
        when(unit.getId()).thenReturn(5);
        return new RoomChatMessage(text, unit, RoomChatMessageBubbles.NORMAL);
    }

    private static String readString(ByteBuf packet) {
        return packet.readCharSequence(packet.readUnsignedShort(), StandardCharsets.UTF_8)
                .toString();
    }
}
