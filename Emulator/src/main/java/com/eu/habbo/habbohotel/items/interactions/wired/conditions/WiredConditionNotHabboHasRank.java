package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import java.sql.ResultSet;
import java.sql.SQLException;

/** "User does not have rank" ({@code wf_cnd_not_habbo_has_rank}): the comparison turned around, per user. */
public class WiredConditionNotHabboHasRank extends WiredConditionHabboHasRank {

    public WiredConditionNotHabboHasRank(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotHabboHasRank(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    protected boolean matchesTarget(int reached) {
        return !super.matchesTarget(reached);
    }
}
