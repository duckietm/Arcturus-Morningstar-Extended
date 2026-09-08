package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.habbo;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import com.eu.habbo.threading.ThreadPooling;
import com.eu.habbo.threading.runnables.RoomUnitWalkToLocation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Bot gives hand item" served the first resolved user only, and when two bots in the room shared
 * the configured name it found neither and did nothing.
 */
class WiredEffectBotGiveHandItemTest {

    @Test
    void everyResolvedUserIsServedByEveryBotWithTheName() throws Exception {
        Room room = mock(Room.class);
        RoomUnit first = mock(RoomUnit.class);
        RoomUnit second = mock(RoomUnit.class);
        // Built before the stubbing: the helpers stub their own mocks, and Mockito reads nested
        // stubbing inside a thenReturn(...) argument as an unfinished one.
        Habbo firstHabbo = habbo("first", first);
        Habbo secondHabbo = habbo("second", second);
        List<Bot> bots = List.of(bot(), bot());
        when(room.getHabbo(first)).thenReturn(firstHabbo);
        when(room.getHabbo(second)).thenReturn(secondHabbo);
        when(room.getBots("Frank")).thenReturn(bots);

        WiredEffectBotGiveHandItem box = new WiredEffectBotGiveHandItem(1, 1, base(), "", 0, 0);
        box.loadWiredData(
                row("{\"bot_name\":\"Frank\",\"item_id\":3,\"delay\":0,\"userSource\":0,\"botSource\":100}"), room);
        ThreadPooling threading = mock(ThreadPooling.class);
        WiredContext ctx = context(room);

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class);
                MockedStatic<WiredSourceUtil> sources = mockStatic(WiredSourceUtil.class)) {
            emulator.when(Emulator::getThreading).thenReturn(threading);
            sources.when(() -> WiredSourceUtil.resolveUsers(ctx, WiredSourceUtil.SOURCE_TRIGGER))
                    .thenReturn(List.of(first, second));

            box.execute(ctx);
        }

        // One walk per (user, bot) pair: two users, two bots.
        verify(threading, times(4)).run(any(RoomUnitWalkToLocation.class));
    }

    private static Bot bot() {
        Bot bot = mock(Bot.class);
        RoomUnit unit = mock(RoomUnit.class);
        when(bot.getRoomUnit()).thenReturn(unit);
        return bot;
    }
}
