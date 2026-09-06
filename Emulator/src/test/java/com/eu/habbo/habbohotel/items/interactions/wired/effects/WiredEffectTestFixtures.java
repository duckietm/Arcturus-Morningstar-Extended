package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.WiredPlatform;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.GameEnvironment;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredServices;
import com.eu.habbo.habbohotel.wired.core.WiredState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mockito.MockedStatic;

/**
 * The mocks the effect tests in this package share: a floor base item, a context with or without an
 * actor, a database row, a user with a client, and the hotel statics a saveData reaches for.
 */
final class WiredEffectTestFixtures {
    private WiredEffectTestFixtures() {}

    static Item base() {
        Item base = mock(Item.class);
        when(base.getType()).thenReturn(FurnitureType.FLOOR);
        when(base.getSpriteId()).thenReturn(4321);
        return base;
    }

    static WiredContext context(Room room) {
        return context(WiredEvent.Type.CUSTOM, room, null);
    }

    static WiredContext context(Room room, RoomUnit actor) {
        return context(WiredEvent.Type.CUSTOM, room, actor);
    }

    static WiredContext context(WiredEvent.Type type, Room room, RoomUnit actor) {
        WiredEvent.Builder builder = WiredEvent.builder(type, room);
        if (actor != null) {
            builder.actor(actor);
        }
        return new WiredContext(builder.build(), null, mock(WiredServices.class), new WiredState(20));
    }

    static ResultSet row(String wiredData) throws SQLException {
        ResultSet row = mock(ResultSet.class);
        when(row.getString("wired_data")).thenReturn(wiredData);
        return row;
    }

    static JsonObject json(InteractionWiredEffect effect) {
        return JsonParser.parseString(effect.getWiredData()).getAsJsonObject();
    }

    /** A user in the room with a name, a client and the given room unit. */
    static Habbo habbo(String username, RoomUnit unit) {
        Habbo habbo = mock(Habbo.class);
        HabboInfo info = mock(HabboInfo.class);
        GameClient client = mock(GameClient.class);
        when(info.getUsername()).thenReturn(username);
        when(habbo.getHabboInfo()).thenReturn(info);
        when(habbo.getClient()).thenReturn(client);
        when(habbo.getRoomUnit()).thenReturn(unit);
        return habbo;
    }

    /** A configuration that allows fifty picked furni and otherwise answers with the caller's default. */
    static ConfigurationManager config() {
        ConfigurationManager config = mock(ConfigurationManager.class);
        when(config.getInt(anyString())).thenReturn(50);
        when(config.getInt(anyString(), anyInt())).thenAnswer(invocation -> invocation.getArgument(1));
        return config;
    }

    /** Makes {@code Emulator.getGameEnvironment()} and {@code Emulator.getConfig()} answer for the room. */
    static void installHotel(MockedStatic<Emulator> emulator, Room room) {
        // Built before the stubbing: each helper stubs its own mocks, and Mockito reads nested
        // stubbing inside a thenReturn(...) argument as an unfinished one.
        GameEnvironment environment = environment(room);
        ConfigurationManager config = config();
        emulator.when(Emulator::getGameEnvironment).thenReturn(environment);
        emulator.when(Emulator::getConfig).thenReturn(config);
    }

    /** Makes {@code WiredPlatform.gameEnvironment()}, which the effect base's getRoom() reads, answer for the room. */
    static void installPlatform(MockedStatic<WiredPlatform> platform, Room room) {
        GameEnvironment environment = environment(room);
        platform.when(WiredPlatform::gameEnvironment).thenReturn(environment);
    }

    private static GameEnvironment environment(Room room) {
        GameEnvironment environment = mock(GameEnvironment.class);
        RoomManager roomManager = mock(RoomManager.class);
        when(roomManager.getRoom(anyInt())).thenReturn(room);
        when(environment.getRoomManager()).thenReturn(roomManager);
        return environment;
    }
}
