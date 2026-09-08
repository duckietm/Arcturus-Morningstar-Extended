package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.config;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.habbo;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.json;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * "Bot follows user" looked at the first resolved user only, and saved the user source raw while
 * its load normalised it, so a save and a reload could disagree.
 */
class WiredEffectBotFollowHabboTest {

    @Test
    void everyResolvedUserIsHandedToTheBot() throws Exception {
        Room room = mock(Room.class);
        RoomUnit first = mock(RoomUnit.class);
        RoomUnit second = mock(RoomUnit.class);
        Habbo firstHabbo = habbo("first", first);
        Habbo secondHabbo = habbo("second", second);
        when(room.getHabbo(first)).thenReturn(firstHabbo);
        when(room.getHabbo(second)).thenReturn(secondHabbo);
        Bot bot = mock(Bot.class);
        when(room.getBots("Frank")).thenReturn(List.of(bot));

        WiredEffectBotFollowHabbo box = new WiredEffectBotFollowHabbo(1, 1, base(), "", 0, 0);
        box.loadWiredData(
                row("{\"bot_name\":\"Frank\",\"mode\":1,\"delay\":0,\"userSource\":0,\"botSource\":100}"), room);

        WiredContext ctx = context(room);

        try (MockedStatic<WiredSourceUtil> sources = mockStatic(WiredSourceUtil.class)) {
            sources.when(() -> WiredSourceUtil.resolveUsers(ctx, WiredSourceUtil.SOURCE_TRIGGER))
                    .thenReturn(List.of(first, second));

            box.execute(ctx);
        }

        verify(bot).startFollowingHabbo(firstHabbo);
        verify(bot).startFollowingHabbo(secondHabbo);
    }

    @Test
    void aUserSourceThatNamesNothingIsSavedAsTheTrigger() throws Exception {
        WiredEffectBotFollowHabbo box = new WiredEffectBotFollowHabbo(1, 1, base(), "", 0, 0);

        ConfigurationManager config = config();

        try (MockedStatic<Emulator> emulator = mockStatic(Emulator.class)) {
            emulator.when(Emulator::getConfig).thenReturn(config);

            box.saveData(new WiredSettings(new int[] {1, 999, 100}, "Frank", new int[0], 0), null);
        }

        assertEquals(WiredSourceUtil.SOURCE_TRIGGER, json(box).get("userSource").getAsInt());
    }
}
