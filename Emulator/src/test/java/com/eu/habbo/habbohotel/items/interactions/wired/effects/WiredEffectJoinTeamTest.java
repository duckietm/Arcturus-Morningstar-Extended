package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.base;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.json;
import static com.eu.habbo.habbohotel.items.interactions.wired.effects.WiredEffectTestFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Gson answers null for a team name it does not know, and the dialog serializer then read the
 * team's type off it. An unknown team is the default one.
 */
class WiredEffectJoinTeamTest {

    @Test
    void aTeamNameThatNamesNothingLoadsAsRed() throws Exception {
        WiredEffectJoinTeam box = new WiredEffectJoinTeam(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("{\"team\":\"PURPLE\",\"teamType\":0,\"delay\":0,\"userSource\":0}"), null);

        assertEquals("RED", json(box).get("team").getAsString());
    }

    @Test
    void aKnownTeamStillLoads() throws Exception {
        WiredEffectJoinTeam box = new WiredEffectJoinTeam(1, 1, base(), "", 0, 0);

        box.loadWiredData(row("{\"team\":\"BLUE\",\"teamType\":0,\"delay\":0,\"userSource\":0}"), null);

        assertEquals("BLUE", json(box).get("team").getAsString());
    }
}
