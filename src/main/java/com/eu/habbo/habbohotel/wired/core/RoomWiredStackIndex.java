package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraOrEval;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraRandom;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraUnseen;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.api.IWiredCondition;
import com.eu.habbo.habbohotel.wired.api.IWiredEffect;
import com.eu.habbo.habbohotel.wired.api.IWiredTrigger;
import com.eu.habbo.habbohotel.wired.api.WiredStack;
import gnu.trove.set.hash.THashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link WiredStackIndex} that builds stacks from {@link RoomSpecialTypes}.
 * <p>
 * This implementation reads wired items from RoomSpecialTypes and builds
 * WiredStack objects on-demand. Since wired items already implement the
 * IWiredTrigger, IWiredCondition, and IWiredEffect interfaces, no adapters
 * are needed.
 * </p>
 * 
 * <h3>Stack Building:</h3>
 * <ol>
 *   <li>Find all triggers of the requested event type</li>
 *   <li>For each trigger, get conditions and effects at the same tile</li>
 *   <li>Check for extras (OR mode, random, unseen) at the tile</li>
 *   <li>Build a WiredStack with the collected components</li>
 * </ol>
 * 
 * @see RoomSpecialTypes
 * @see WiredStack
 */
public final class RoomWiredStackIndex implements WiredStackIndex {

    /** Cache of built stacks per room and event type */
    private final ConcurrentHashMap<Integer, Map<WiredEvent.Type, List<WiredStack>>> cache;
    
    /** Whether to use caching (can be disabled for testing) */
    private final boolean useCache;

    /**
     * Create a new index with caching enabled.
     */
    public RoomWiredStackIndex() {
        this(true);
    }

    /**
     * Create a new index with optional caching.
     * @param useCache true to cache built stacks, false to rebuild on every request
     */
    public RoomWiredStackIndex(boolean useCache) {
        this.useCache = useCache;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public List<WiredStack> getStacks(Room room, WiredEvent.Type type) {
        if (room == null || type == null) {
            return Collections.emptyList();
        }

        // Check cache first
        if (useCache) {
            Map<WiredEvent.Type, List<WiredStack>> roomCache = cache.get(room.getId());
            if (roomCache != null) {
                List<WiredStack> cached = roomCache.get(type);
                if (cached != null) {
                    return cached;
                }
            }
        }

        // Build stacks for this event type
        List<WiredStack> stacks = buildStacks(room, type);

        // Cache the result
        if (useCache) {
            cache.computeIfAbsent(room.getId(), k -> new ConcurrentHashMap<>())
                    .put(type, stacks);
        }

        return stacks;
    }

    @Override
    public void rebuild(Room room) {
        if (room == null) return;
        invalidateAll(room);
    }

    @Override
    public void invalidate(Room room, RoomTile tile) {
        if (room == null) return;
        // For simplicity, invalidate all cached stacks for the room
        // A more sophisticated implementation could track which stacks use which tiles
        invalidateAll(room);
    }

    @Override
    public void invalidateAll(Room room) {
        if (room == null) return;
        cache.remove(room.getId());
    }

    @Override
    public boolean isCached(Room room) {
        return room != null && cache.containsKey(room.getId());
    }

    /**
     * Build all wired stacks for a specific event type in a room.
     */
    private List<WiredStack> buildStacks(Room room, WiredEvent.Type type) {
        RoomSpecialTypes specialTypes = room.getRoomSpecialTypes();
        if (specialTypes == null) {
            return Collections.emptyList();
        }

        // Get legacy trigger type
        WiredTriggerType legacyType = type.toLegacyType();
        if (legacyType == null) {
            return Collections.emptyList();
        }

        // Get all triggers of this type
        THashSet<InteractionWiredTrigger> triggers = specialTypes.getTriggers(legacyType);
        if (triggers == null || triggers.isEmpty()) {
            return Collections.emptyList();
        }

        List<WiredStack> stacks = new ArrayList<>();

        for (InteractionWiredTrigger trigger : triggers) {
            // Avoid processing multiple triggers at the same tile multiple times
            // (we build one stack per trigger, but share conditions/effects at same location)
            short x = trigger.getX();
            short y = trigger.getY();
            
            // Build the stack for this trigger
            WiredStack stack = buildStack(room, specialTypes, trigger, x, y);
            if (stack != null) {
                stacks.add(stack);
            }
        }

        return stacks.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(stacks);
    }

    /**
     * Build a single wired stack for a trigger at a specific location.
     */
    private WiredStack buildStack(Room room, RoomSpecialTypes specialTypes,
                                   InteractionWiredTrigger trigger, short x, short y) {
        // The trigger already implements IWiredTrigger
        IWiredTrigger wrappedTrigger = trigger;

        // Get conditions at this location
        THashSet<InteractionWiredCondition> rawConditions = specialTypes.getConditions(x, y);
        List<IWiredCondition> conditions = collectConditions(rawConditions);

        // Get effects at this location
        THashSet<InteractionWiredEffect> rawEffects = specialTypes.getEffects(x, y);
        List<IWiredEffect> effects = collectEffects(rawEffects);

        // Check for extras
        boolean useOrMode = specialTypes.hasExtraType(x, y, WiredExtraOrEval.class);
        boolean useRandom = specialTypes.hasExtraType(x, y, WiredExtraRandom.class);
        boolean useUnseen = specialTypes.hasExtraType(x, y, WiredExtraUnseen.class);

        return new WiredStack(
                trigger,
                wrappedTrigger,
                conditions,
                effects,
                useOrMode,
                useRandom,
                useUnseen
        );
    }

    /**
     * Collect conditions into a list (they already implement IWiredCondition).
     */
    private List<IWiredCondition> collectConditions(THashSet<InteractionWiredCondition> rawConditions) {
        if (rawConditions == null || rawConditions.isEmpty()) {
            return Collections.emptyList();
        }

        List<IWiredCondition> conditions = new ArrayList<>(rawConditions.size());
        for (InteractionWiredCondition condition : rawConditions) {
            conditions.add(condition);
        }
        return conditions;
    }

    /**
     * Collect effects into a list (they already implement IWiredEffect).
     */
    private List<IWiredEffect> collectEffects(THashSet<InteractionWiredEffect> rawEffects) {
        if (rawEffects == null || rawEffects.isEmpty()) {
            return Collections.emptyList();
        }

        List<IWiredEffect> effects = new ArrayList<>(rawEffects.size());
        for (InteractionWiredEffect effect : rawEffects) {
            effects.add(effect);
        }
        return effects;
    }

    /**
     * Clear all cached data.
     */
    public void clearAll() {
        cache.clear();
    }

    /**
     * Get the number of rooms currently cached.
     * @return cached room count
     */
    public int getCachedRoomCount() {
        return cache.size();
    }
}
