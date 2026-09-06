package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Counterpart of {@link WiredEffectHideFurni}: restores full opacity and click handling. */
public class WiredEffectUnhideFurni extends WiredEffectHideFurni {
    public static final WiredEffectType type = WiredEffectType.UNHIDE_FURNI;

    public WiredEffectUnhideFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectUnhideFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    protected boolean hides() {
        return false;
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }
}
