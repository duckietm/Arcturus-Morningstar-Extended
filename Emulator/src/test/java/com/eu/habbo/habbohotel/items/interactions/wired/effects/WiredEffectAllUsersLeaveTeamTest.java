package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.context;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.wired.WiredGame;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredSourceUtil;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

/**
 * "All users leave team" on its default source empties every team in the room, which is what its
 * name says. The dialog also offers narrower user sources, which were saved and then ignored: any
 * source still removed everyone.
 */
class WiredEffectAllUsersLeaveTeamTest {

    @Test
    void theDefaultSourceEmptiesEveryTeamInTheRoom() throws Exception {
        Room room = mock(Room.class);
        Game game = mock(Game.class);
        RoomUnit actorUnit = mock(RoomUnit.class);
        Habbo actor = playing(room, game, actorUnit);
        Habbo bystander = playing(room, game, mock(RoomUnit.class));
        ConcurrentHashMap<Integer, Habbo> present = new ConcurrentHashMap<>();
        present.put(1, actor);
        present.put(2, bystander);
        when(room.getCurrentHabbos()).thenReturn(present);

        WiredEffectAllUsersLeaveTeam box = box(room, WiredSourceUtil.SOURCE_TRIGGER);
        box.execute(context(room, actorUnit));

        verify(game).removeHabbo(actor);
        verify(game).removeHabbo(bystander);
    }

    @Test
    void aNarrowerSourceOnlyRemovesTheUsersItResolves() throws Exception {
        Room room = mock(Room.class);
        Game game = mock(Game.class);
        RoomUnit actorUnit = mock(RoomUnit.class);
        Habbo actor = playing(room, game, actorUnit);
        Habbo bystander = playing(room, game, mock(RoomUnit.class));
        ConcurrentHashMap<Integer, Habbo> present = new ConcurrentHashMap<>();
        present.put(1, actor);
        present.put(2, bystander);
        when(room.getCurrentHabbos()).thenReturn(present);

        WiredEffectAllUsersLeaveTeam box = box(room, WiredSourceUtil.SOURCE_SIGNAL);
        box.execute(context(WiredEvent.Type.SIGNAL_RECEIVED, room, actorUnit));

        verify(game).removeHabbo(actor);
        verify(game, never()).removeHabbo(bystander);
    }

    @Test
    void aLegacyRowThatIsNotANumberLoadsWithNoDelay() throws Exception {
        WiredEffectAllUsersLeaveTeam box = new WiredEffectAllUsersLeaveTeam(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("abc"), null);
        assertEquals(0, box.getDelay());

        box.loadWiredData(row("7"), null);
        assertEquals(7, box.getDelay());
    }

    private static WiredEffectAllUsersLeaveTeam box(Room room, int userSource) throws Exception {
        WiredEffectAllUsersLeaveTeam box = new WiredEffectAllUsersLeaveTeam(1, 1, base(), "", 0, 0);
        box.loadWiredData(row("{\"delay\":0,\"userSource\":" + userSource + "}"), room);
        return box;
    }

    private static Habbo playing(Room room, Game game, RoomUnit unit) {
        Habbo habbo = mock(Habbo.class);
        HabboInfo info = mock(HabboInfo.class);
        doReturn(WiredGame.class).when(info).getCurrentGame();
        when(habbo.getHabboInfo()).thenReturn(info);
        when(room.getHabbo(unit)).thenReturn(habbo);
        when(room.getGame(WiredGame.class)).thenReturn(game);
        return habbo;
    }
}
