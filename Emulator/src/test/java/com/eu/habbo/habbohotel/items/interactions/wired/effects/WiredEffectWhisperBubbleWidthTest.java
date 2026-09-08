package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.wired.WiredEffectDataComposer;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The "show message" box takes a fourth int, the bubble width the message should use: -1 keeps the
 * room setting, 0/1/2 are wide/normal/thin as the room setting names them. A box saved before the
 * slot existed keeps the room setting.
 */
class WiredEffectWhisperBubbleWidthTest {

    // saveData clamps the message against the hotel configuration, which nothing installs in a unit
    // test; a minimal one with the two limits is enough.
    @BeforeAll
    static void installConfiguration() throws Exception {
        Path config = Files.createTempFile("polaris-whisper-test", ".ini");
        Files.writeString(config, "hotel.wired.show_message.max_length=200\nhotel.wired.show_message.max_lines=8\n");
        setEmulatorConfig(new ConfigurationManager(config.toString()));
    }

    @AfterAll
    static void removeConfiguration() throws Exception {
        setEmulatorConfig(null);
    }

    private static void setEmulatorConfig(ConfigurationManager manager) throws Exception {
        Field field = Emulator.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, manager);
    }

    @Test
    void aBoxSavedWithThreeIntsKeepsTheRoomSetting() throws Exception {
        WiredEffectWhisper effect = saved(new int[] {0, 0, 34});

        assertEquals(-1, effect.getBubbleWidthOverride());
    }

    @Test
    void theFourthIntIsTheBubbleWidth() throws Exception {
        assertEquals(2, saved(new int[] {0, 0, 34, 2}).getBubbleWidthOverride());
        assertEquals(0, saved(new int[] {0, 0, 34, 0}).getBubbleWidthOverride());
    }

    @Test
    void aWidthThatNamesNothingKeepsTheRoomSetting() throws Exception {
        assertEquals(-1, saved(new int[] {0, 0, 34, 9}).getBubbleWidthOverride());
        assertEquals(-1, saved(new int[] {0, 0, 34, -3}).getBubbleWidthOverride());
    }

    @Test
    void theDialogGetsFourIntsBackWithTheWidthLast() throws Exception {
        WiredEffectWhisper effect = saved(new int[] {0, 1, 34, 1});
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
            assertEquals(4, packet.readInt());
            assertEquals(0, packet.readInt());
            assertEquals(1, packet.readInt());
            assertEquals(34, packet.readInt());
            assertEquals(1, packet.readInt());
        } finally {
            packet.release();
        }
    }

    private static WiredEffectWhisper saved(int[] intParams) throws Exception {
        Item boxBase = mock(Item.class);
        when(boxBase.getType()).thenReturn(FurnitureType.FLOOR);
        when(boxBase.getSpriteId()).thenReturn(1234);
        WiredEffectWhisper effect = new WiredEffectWhisper(77, 1, boxBase, "", 0, 0);

        Habbo habbo = mock(Habbo.class);
        when(habbo.hasPermission(anyString())).thenReturn(true);
        GameClient client = mock(GameClient.class);
        when(client.getHabbo()).thenReturn(habbo);

        effect.saveData(new WiredSettings(intParams, "hello", new int[0], 0), client);
        return effect;
    }
}
