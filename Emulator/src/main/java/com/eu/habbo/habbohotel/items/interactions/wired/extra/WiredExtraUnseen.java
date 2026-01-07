package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WiredExtraUnseen extends InteractionWiredExtra {
    /**
     * Maximum number of effect IDs to track to prevent memory leaks.
     * When limit is reached, oldest entries are removed automatically.
     */
    private static final int MAX_SEEN_LIST_SIZE = 1000;
    
    /**
     * Thread-safe set of seen effect IDs. Uses LinkedHashSet for insertion order
     * to support LRU-style eviction when max size is reached.
     */
    private final Set<Integer> seenList = new LinkedHashSet<>();

    public WiredExtraUnseen(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraUnseen(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return null;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {

    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {

    }

    @Override
    public void onPickUp() {
        synchronized (this.seenList) {
            this.seenList.clear();
        }
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) throws Exception {

    }

    @Override
    public void onMove(Room room, RoomTile oldLocation, RoomTile newLocation) {
        super.onMove(room, oldLocation, newLocation);
        synchronized (this.seenList) {
            this.seenList.clear();
        }
    }

    public InteractionWiredEffect getUnseenEffect(List<InteractionWiredEffect> effects) {
        synchronized (this.seenList) {
            List<InteractionWiredEffect> unseenEffects = new ArrayList<>();
            for (InteractionWiredEffect effect : effects) {
                if (!this.seenList.contains(effect.getId())) {
                    unseenEffects.add(effect);
                }
            }

            InteractionWiredEffect effect = null;
            if (!unseenEffects.isEmpty()) {
                effect = unseenEffects.get(0);
            } else {
                this.seenList.clear();

                if (!effects.isEmpty()) {
                    effect = effects.get(0);
                }
            }

            if (effect != null) {
                // Enforce max size limit to prevent memory leaks
                if (this.seenList.size() >= MAX_SEEN_LIST_SIZE) {
                    // Remove oldest entry (first in insertion order)
                    Integer oldest = this.seenList.iterator().next();
                    this.seenList.remove(oldest);
                }
                this.seenList.add(effect.getId());
            }
            return effect;
        }
    }
    
    /**
     * Gets the current size of the seen list.
     * @return the number of tracked effect IDs
     */
    public int getSeenListSize() {
        synchronized (this.seenList) {
            return this.seenList.size();
        }
    }
    
    /**
     * Clears the seen list.
     */
    public void clearSeenList() {
        synchronized (this.seenList) {
            this.seenList.clear();
        }
    }
}
