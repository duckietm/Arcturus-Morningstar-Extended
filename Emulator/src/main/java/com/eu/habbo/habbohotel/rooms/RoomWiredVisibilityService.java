package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RoomFloorItemsComposer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the two halves of "wired is hidden" together: what the clients are shown, and what the
 * room's tiles collide with. Hiding used to be visuals only, which left an invisible wall on every
 * tile a wired box stood on and floated avatars over the stacked ones.
 */
final class RoomWiredVisibilityService {

    private final Room room;

    /** What the clients and the tile states currently reflect. */
    private boolean applied;

    RoomWiredVisibilityService(Room room) {
        this.room = room;
    }

    void setHidden(boolean hidden) {
        this.room.updateHideWiredState(hidden);
        this.refresh();
    }

    /**
     * Recomputes whether wired is hidden and, when that changed, republishes or removes the boxes
     * and recalculates the tiles they stand on, so walking matches what the room looks like.
     */
    synchronized void refresh() {
        boolean hidden = this.room.isHideWired() || RoomHideWiredSupport.isActive(this.room);

        if (hidden == this.applied) {
            this.room.setWiredHiddenFlag(hidden);
            return;
        }

        this.applied = hidden;
        // Set before the tiles are recalculated: the tile maths reads this flag.
        this.room.setWiredHiddenFlag(hidden);

        List<HabboItem> wired = this.wiredItems();

        if (hidden) {
            this.remove(wired);
        } else {
            this.publish(wired);
        }

        this.room.updateTiles(this.tilesUnder(wired));
    }

    private List<HabboItem> wiredItems() {
        RoomSpecialTypes specialTypes = this.room.getRoomSpecialTypes();
        List<HabboItem> items = new ArrayList<>();
        items.addAll(specialTypes.getTriggers());
        items.addAll(specialTypes.getEffects());
        items.addAll(specialTypes.getConditions());
        items.addAll(specialTypes.getExtras());

        return items;
    }

    private Set<RoomTile> tilesUnder(Collection<? extends HabboItem> items) {
        Set<RoomTile> tiles = new HashSet<>();
        RoomLayout layout = this.room.getLayout();

        if (layout == null) {
            return tiles;
        }

        for (HabboItem item : items) {
            tiles.addAll(item.getOccupyingTiles(layout));
        }

        return tiles;
    }

    private void remove(Collection<? extends HabboItem> items) {
        for (HabboItem item : items) {
            this.room.sendComposer(new RemoveFloorItemComposer(item).compose());
        }
    }

    private void publish(Collection<? extends HabboItem> items) {
        this.room.sendComposer(new RoomFloorItemsComposer(this.room.getFurniOwnerNames(), items).compose());
    }
}
