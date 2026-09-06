package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Server-side log that runs only when the stack's conditions FAIL (the negative branch). Identical to
 * {@link WiredEffectLog}, and it answers the same {@link WiredEffectType#EFFECT_MESSAGE} type because
 * the type code only picks the client dialog. WiredEffectPlanner recognises the negative branch by
 * this class, not by the type.
 */
public class WiredEffectNegativeLog extends WiredEffectLog {
    public static final WiredEffectType type = WiredEffectType.EFFECT_MESSAGE;

    public WiredEffectNegativeLog(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectNegativeLog(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }
}
