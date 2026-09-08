package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import java.sql.ResultSet;
import java.sql.SQLException;

/** "User does not have X points on the scoreboard" ({@code wf_cnd_not_x_points_leaderboard}): the comparison turned around, per user. */
public class WiredConditionNotHabboHasHighscorePoints extends WiredConditionHabboHasHighscorePoints {

    public WiredConditionNotHabboHasHighscorePoints(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotHabboHasHighscorePoints(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    protected boolean matchesTarget(int best) {
        return !super.matchesTarget(best);
    }
}
