package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * BSS "I furni selezionati eseguono lo scontro" (wf_act_tiles): every resolved furni fires the
 * collision trigger as if it had bumped into the users standing on it. When nobody stands on a
 * furni, the user that triggered this stack is used so the collision chain still runs.
 */
public class WiredEffectFurniCollision extends WiredEffectSelectedFurniBase {
    public static final WiredEffectType type = WiredEffectType.FURNI_COLLISION;

    public WiredEffectFurniCollision(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectFurniCollision(
            int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    protected void apply(WiredContext ctx, List<HabboItem> targets) {
        Room room = ctx.room();

        for (HabboItem item : targets) {
            if (item == null || item.getId() == this.getId()) continue;

            Set<RoomUnit> units = new LinkedHashSet<>();
            for (Habbo habbo : room.getHabbosOnItem(item)) {
                if (habbo != null && habbo.getRoomUnit() != null) units.add(habbo.getRoomUnit());
            }
            if (units.isEmpty() && ctx.actor().isPresent()) {
                units.add(ctx.actor().get());
            }

            for (RoomUnit unit : units) {
                WiredEvent event = WiredEvent.builder(WiredEvent.Type.BOT_COLLISION, room)
                        .actor(unit)
                        .sourceItem(item)
                        .tile(unit.getCurrentLocation())
                        .triggeredByEffect(true)
                        .build();
                WiredManager.handleEvent(event);
            }
        }
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }
}
