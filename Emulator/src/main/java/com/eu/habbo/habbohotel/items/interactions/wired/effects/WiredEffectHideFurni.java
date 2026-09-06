package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.WiredOpacityState;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.messages.outgoing.rooms.WiredFurniOpacityComposer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Hides (wf_act_hide_trg_item) or shows again (wf_act_unhide_items) the resolved furni for
 * everyone, through the same opacity runtime the opacity effect uses, so both effects stay in
 * sync with each other.
 */
public class WiredEffectHideFurni extends WiredEffectSelectedFurniBase {
    public static final WiredEffectType type = WiredEffectType.HIDE_FURNI;

    private static final int HIDDEN = 0;
    private static final int VISIBLE = 100;

    public WiredEffectHideFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectHideFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    protected boolean hides() {
        return true;
    }

    @Override
    protected void apply(WiredContext ctx, List<HabboItem> targets) {
        Room room = ctx.room();
        int opacity = this.hides() ? HIDDEN : VISIBLE;
        List<WiredOpacityState> applied = room.getWiredRuntime().applyGlobalOpacity(targets, opacity, this.hides());
        if (applied.isEmpty()) return;

        List<Integer> itemIds = targets.stream().map(HabboItem::getId).toList();
        for (Habbo recipient : room.getHabbos()) {
            if (recipient == null || recipient.getClient() == null) continue;

            List<WiredOpacityState> effective =
                    room.getWiredRuntime().effectiveOpacity(recipient.getHabboInfo().getId(), itemIds);
            recipient.getClient().sendResponse(new WiredFurniOpacityComposer(room.getId(), effective, 0, 0));
        }
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }
}
