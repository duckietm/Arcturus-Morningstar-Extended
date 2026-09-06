package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionDice;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.threading.runnables.RandomDiceNumber;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Rolls every selected die exactly like a double click, honouring the room dice lock. */
public class WiredEffectRollDice extends WiredEffectSelectedFurniBase {
    public static final WiredEffectType type = WiredEffectType.ROLL_DICE;

    public WiredEffectRollDice(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectRollDice(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    protected void apply(WiredContext ctx, List<HabboItem> targets) {
        Room room = ctx.room();
        if (com.eu.habbo.habbohotel.rooms.RoomDiceDisableSupport.isActive(room)) return;

        for (HabboItem item : targets) {
            if (!(item instanceof InteractionDice dice) || "-1".equalsIgnoreCase(dice.getExtradata())) continue;

            dice.setExtradata("-1");
            room.updateItemState(dice);
            Emulator.getThreading().run(dice);
            Emulator.getThreading().run(new RandomDiceNumber(dice, room, dice.getBaseItem().getStateCount()), 1500);
        }
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }
}
