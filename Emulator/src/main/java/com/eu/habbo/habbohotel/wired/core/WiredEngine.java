package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredConditionOperator;
import com.eu.habbo.habbohotel.wired.api.IWiredCondition;
import com.eu.habbo.habbohotel.wired.api.IWiredEffect;
import com.eu.habbo.habbohotel.wired.api.WiredStack;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericAlertComposer;
import com.eu.habbo.plugin.events.furniture.wired.WiredStackExecutedEvent;
import com.eu.habbo.plugin.events.furniture.wired.WiredStackTriggeredEvent;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The central engine for processing wired events.
 * <p>
 * This is the single entry point for all wired execution in the new architecture.
 * It receives {@link WiredEvent} objects, finds matching stacks via {@link WiredStackIndex},
 * evaluates conditions, and executes effects.
 * </p>
 * 
 * <h3>Execution Flow:</h3>
 * <ol>
 *   <li>Receive event via {@link #handleEvent(WiredEvent)}</li>
 *   <li>Find candidate stacks for the event type</li>
 *   <li>For each stack, check if trigger matches</li>
 *   <li>Evaluate all conditions (respecting AND/OR mode)</li>
 *   <li>Execute effects (respecting random/unseen modifiers)</li>
 *   <li>Handle delays for timed effects</li>
 * </ol>
 * 
 * <h3>Safety Features:</h3>
 * <ul>
 *   <li>Step limits via {@link WiredState} prevent infinite loops</li>
 *   <li>Effect cooldowns prevent rapid re-triggering</li>
 *   <li>Exceptions are caught and logged, not propagated</li>
 * </ul>
 * 
 * @see WiredEvent
 * @see WiredContext
 * @see WiredStackIndex
 */
public final class WiredEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(WiredEngine.class);
    
    /** Maximum recursion depth to prevent infinite loops (e.g., collision + chase) */
    public static int MAX_RECURSION_DEPTH = 10;
    
    /** Maximum events of same type per room within rate limit window before banning */
    public static int MAX_EVENTS_PER_WINDOW = 100;
    
    /** Time window for counting rapid events (milliseconds) */
    public static long RATE_LIMIT_WINDOW_MS = 10000;
    
    /** Duration to ban wired execution in a room after abuse detected (milliseconds) */
    public static long WIRED_BAN_DURATION_MS = 600000;

    private final WiredServices services;
    private final WiredStackIndex index;
    private final int maxStepsPerStack;
    
    /** Track unseen effect indices per room+tile for round-robin selection */
    private final ConcurrentHashMap<String, Integer> unseenIndices;
    
    /** Track recursion depth per room to prevent infinite loops */
    private final ConcurrentHashMap<Integer, Integer> roomRecursionDepth;
    
    /** Track event timestamps per room+eventType for rate limiting: key = "roomId:eventType" */
    private final ConcurrentHashMap<String, EventRateTracker> eventRateLimiters;
    
    /** Track rooms that are banned from wired execution: roomId -> ban expiry timestamp */
    private final ConcurrentHashMap<Integer, Long> bannedRooms;

    /**
     * Create a new wired engine.
     * 
     * @param services the services for performing side effects
     * @param index the stack index for finding matching stacks
     * @param maxStepsPerStack maximum steps per stack execution (loop protection)
     */
    public WiredEngine(WiredServices services, WiredStackIndex index, int maxStepsPerStack) {
        if (services == null) throw new IllegalArgumentException("Services cannot be null");
        if (index == null) throw new IllegalArgumentException("Index cannot be null");
        if (maxStepsPerStack <= 0) throw new IllegalArgumentException("Max steps must be positive");
        
        this.services = services;
        this.index = index;
        this.maxStepsPerStack = maxStepsPerStack;
        this.unseenIndices = new ConcurrentHashMap<>();
        this.roomRecursionDepth = new ConcurrentHashMap<>();
        this.eventRateLimiters = new ConcurrentHashMap<>();
        this.bannedRooms = new ConcurrentHashMap<>();
    }

    /**
     * Handle a wired event by finding and executing matching stacks.
     * 
     * @param event the event to handle
     * @return true if any stack was triggered (useful for SAY_SOMETHING to suppress message)
     */
    public boolean handleEvent(WiredEvent event) {
        if (event == null) {
            return false;
        }

        Room room = event.getRoom();
        if (room == null || !room.isLoaded()) {
            return false;
        }
        
        int roomId = room.getId();
        
        // Check if room is banned from wired execution
        if (isRoomBanned(roomId)) {
            return false;
        }
        
        // Check rate limiting to prevent rapid-fire event spam (e.g., collision + chase loop)
        if (isRateLimited(roomId, room, event.getType())) {
            // Room has been banned, all events will be dropped
            return false;
        }
        
        // Check and increment recursion depth to prevent infinite loops
        int currentDepth = roomRecursionDepth.getOrDefault(roomId, 0);
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            LOGGER.warn("Wired recursion limit reached in room {} (depth: {}). " +
                    "Possible infinite loop detected (e.g., collision + chase). Aborting.", roomId, currentDepth);
            debug(room, "RECURSION LIMIT REACHED - aborting to prevent crash");
            return false;
        }
        roomRecursionDepth.put(roomId, currentDepth + 1);
        
        try {
            return handleEventInternal(event, room);
        } finally {
            // Decrement recursion depth
            int newDepth = roomRecursionDepth.getOrDefault(roomId, 1) - 1;
            if (newDepth <= 0) {
                roomRecursionDepth.remove(roomId);
            } else {
                roomRecursionDepth.put(roomId, newDepth);
            }
        }
    }
    
    /**
     * Internal event handling after recursion check.
     */
    private boolean handleEventInternal(WiredEvent event, Room room) {

        // Find candidate stacks for this event type
        List<WiredStack> stacks = index.getStacks(room, event.getType());
        if (stacks.isEmpty()) {
            return false;
        }

        debug(room, "Processing {} stacks for event type {}", stacks.size(), event.getType());

        boolean anyTriggered = false;
        long currentTime = System.currentTimeMillis();

        for (WiredStack stack : stacks) {
            try {
                boolean triggered = processStack(stack, event, currentTime);
                if (triggered) {
                    anyTriggered = true;
                }
            } catch (WiredLimitException limitEx) {
                debug(room, "Stack execution stopped (limit): {}", limitEx.getMessage());
            } catch (Exception ex) {
                LOGGER.error("Error processing wired stack in room {}: {}", room.getId(), ex.getMessage(), ex);
                debug(room, "Stack error: {}", ex.getMessage());
            }
        }

        return anyTriggered;
    }

    /**
     * Process a single wired stack.
     */
    private boolean processStack(WiredStack stack, WiredEvent event, long currentTime) {
        Room room = event.getRoom();

        // Check if trigger matches
        if (!stack.trigger().matches(stack.triggerItem(), event)) {
            return false;
        }

        // Check if trigger requires actor
        if (stack.trigger().requiresActor() && !event.getActor().isPresent()) {
            return false;
        }

        // Create execution context with stack reference
        WiredState state = new WiredState(maxStepsPerStack);
        WiredContext ctx = new WiredContext(event, stack.triggerItem(), stack, services, state, null);

        // Initial step for trigger
        state.step();
        
        // Activate the trigger box animation
        if (stack.triggerItem() instanceof InteractionWiredTrigger) {
            InteractionWiredTrigger trigger = (InteractionWiredTrigger) stack.triggerItem();
            trigger.activateBox(room, event.getActor().orElse(null), currentTime);
        }

        debug(room, "Trigger matched: {} at item {} (conditions: {}, effects: {})", 
              event.getType(), 
              stack.triggerItem() != null ? stack.triggerItem().getId() : "null",
              stack.conditions().size(),
              stack.effects().size());
        
        // Activate extras (for their animation)
        activateExtras(room, stack.triggerItem(), event.getActor().orElse(null), currentTime);

        // Evaluate conditions
        if (stack.hasConditions()) {
            debug(room, "Evaluating {} conditions...", stack.conditions().size());
            boolean conditionsPassed = evaluateConditions(stack, ctx);
            debug(room, "Conditions result: {}", conditionsPassed ? "PASSED" : "FAILED");
            if (!conditionsPassed) {
                debug(room, "Conditions failed, aborting stack");
                return false;
            }
        } else {
            debug(room, "No conditions in stack, proceeding to effects");
        }

        // Fire plugin event (WiredStackTriggeredEvent)
        if (!fireTriggeredEvent(stack, event)) {
            debug(room, "Stack cancelled by plugin");
            return false;
        }

        // Execute effects
        if (stack.hasEffects()) {
            executeEffects(stack, ctx, currentTime);
        }

        // Fire executed event
        fireExecutedEvent(stack, event);

        return true;
    }

    /**
     * Evaluate all conditions in a stack.
     */
    private boolean evaluateConditions(WiredStack stack, WiredContext ctx) {
        List<IWiredCondition> conditions = stack.conditions();
        
        if (stack.useOrMode()) {
            // OR mode: at least one condition must pass
            return evaluateOrMode(conditions, ctx);
        } else {
            // Standard mode: use individual operators
            return evaluateStandardMode(conditions, ctx);
        }
    }

    /**
     * Evaluate conditions in OR mode (any pass = success).
     */
    private boolean evaluateOrMode(List<IWiredCondition> conditions, WiredContext ctx) {
        // Group by condition type (for legacy compatibility)
        Map<String, Boolean> typeResults = new HashMap<>();
        
        for (IWiredCondition condition : conditions) {
            ctx.state().step();
            
            String typeName = condition.getClass().getSimpleName();
            if (!typeResults.containsKey(typeName) && condition.evaluate(ctx)) {
                typeResults.put(typeName, true);
            }
        }
        
        // At least one condition type must have passed
        return !typeResults.isEmpty();
    }

    /**
     * Evaluate conditions in standard mode using operators.
     */
    private boolean evaluateStandardMode(List<IWiredCondition> conditions, WiredContext ctx) {
        Room room = ctx.room();
        
        // First pass: collect all OR conditions that passed
        Map<String, Boolean> orResults = new HashMap<>();
        for (IWiredCondition condition : conditions) {
            if (condition.operator() == WiredConditionOperator.OR) {
                ctx.state().step();
                String typeName = condition.getClass().getSimpleName();
                boolean result = condition.evaluate(ctx);
                debug(room, "  Condition (OR) {}: {}", typeName, result ? "PASS" : "FAIL");
                if (!orResults.containsKey(typeName) && result) {
                    orResults.put(typeName, true);
                }
            }
        }

        // Second pass: verify all conditions
        for (IWiredCondition condition : conditions) {
            boolean passes;
            String typeName = condition.getClass().getSimpleName();
            
            if (condition.operator() == WiredConditionOperator.OR) {
                // OR: passes if any of same type passed
                passes = orResults.containsKey(typeName);
                debug(room, "  Condition (OR check) {}: {}", typeName, passes ? "PASS" : "FAIL");
            } else {
                // AND: must evaluate and pass
                ctx.state().step();
                passes = condition.evaluate(ctx);
                debug(room, "  Condition (AND) {}: {}", typeName, passes ? "PASS" : "FAIL");
            }
            
            if (!passes) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Execute effects in a stack.
     */
    private void executeEffects(WiredStack stack, WiredContext ctx, long currentTime) {
        List<IWiredEffect> effects = stack.effects();
        
        if (effects.isEmpty()) {
            return;
        }

        // Determine which effects to execute
        List<IWiredEffect> toExecute;
        
        if (stack.useRandom()) {
            // Random mode: pick one random effect
            int randomIndex = new Random().nextInt(effects.size());
            toExecute = Collections.singletonList(effects.get(randomIndex));
            debug(ctx.room(), "Random mode: selected effect {}/{}", randomIndex + 1, effects.size());
        } else if (stack.useUnseen()) {
            // Unseen mode: round-robin selection
            int index = getNextUnseenIndex(stack, effects.size());
            toExecute = Collections.singletonList(effects.get(index));
            debug(ctx.room(), "Unseen mode: selected effect {}/{}", index + 1, effects.size());
        } else {
            // Normal mode: execute all in random order
            toExecute = new ArrayList<>(effects);
            Collections.shuffle(toExecute);
        }

        // Execute selected effects
        for (IWiredEffect effect : toExecute) {
            // Check if effect requires actor
            if (effect.requiresActor() && !ctx.hasActor()) {
                continue;
            }

            // Handle delay
            int delay = effect.getDelay();
            if (delay > 0) {
                // Schedule delayed execution
                scheduleDelayedEffect(effect, ctx, delay, currentTime);
            } else {
                // Execute immediately
                ctx.state().step();
                try {
                    effect.execute(ctx);
                    
                    // Activate box animation after execution
                    if (effect instanceof InteractionWiredEffect) {
                        InteractionWiredEffect wiredEffect = (InteractionWiredEffect) effect;
                        wiredEffect.setCooldown(currentTime);
                        wiredEffect.activateBox(ctx.room(), ctx.actor().orElse(null), currentTime);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error executing effect: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Schedule a delayed effect execution.
     */
    private void scheduleDelayedEffect(IWiredEffect effect, WiredContext ctx, int delay, long currentTime) {
        // Delay is in 500ms ticks
        long delayMs = delay * 500L;
        Room room = ctx.room();
        RoomUnit actor = ctx.actor().orElse(null);
        
        Emulator.getThreading().run(() -> {
            if (!room.isLoaded() || room.getHabbos().isEmpty()) {
                return;
            }
            
            try {
                effect.execute(ctx);
                
                // Activate box animation after execution
                if (effect instanceof InteractionWiredEffect) {
                    InteractionWiredEffect wiredEffect = (InteractionWiredEffect) effect;
                    wiredEffect.setCooldown(System.currentTimeMillis());
                    wiredEffect.activateBox(room, actor, System.currentTimeMillis());
                }
            } catch (Exception e) {
                LOGGER.warn("Error executing delayed effect: {}", e.getMessage());
            }
        }, delayMs);
    }

    /**
     * Get the next unseen index for round-robin selection.
     */
    private int getNextUnseenIndex(WiredStack stack, int effectCount) {
        String key = stack.triggerItem() != null 
                ? String.valueOf(stack.triggerItem().getId())
                : "default";
        
        int current = unseenIndices.getOrDefault(key, -1);
        int next = (current + 1) % effectCount;
        unseenIndices.put(key, next);
        
        return next;
    }

    /**
     * Fire the WiredStackTriggeredEvent for plugin compatibility.
     */
    private boolean fireTriggeredEvent(WiredStack stack, WiredEvent event) {
        // Build legacy collections for event
        if (stack.triggerItem() instanceof InteractionWiredTrigger) {
            // This event is checked for cancellation
            THashSet<InteractionWiredEffect> legacyEffects = new THashSet<>();
            THashSet<InteractionWiredCondition> legacyConditions = new THashSet<>();
            
            // Extract effects (all effects should now implement both interfaces)
            for (IWiredEffect eff : stack.effects()) {
                if (eff instanceof InteractionWiredEffect) {
                    legacyEffects.add((InteractionWiredEffect) eff);
                }
            }
            for (IWiredCondition cond : stack.conditions()) {
                if (cond instanceof InteractionWiredCondition) {
                    legacyConditions.add((InteractionWiredCondition) cond);
                }
            }
            
            WiredStackTriggeredEvent triggeredEvent = new WiredStackTriggeredEvent(
                    event.getRoom(),
                    event.getActor().orElse(null),
                    (InteractionWiredTrigger) stack.triggerItem(),
                    legacyEffects,
                    legacyConditions
            );
            
            return !Emulator.getPluginManager().fireEvent(triggeredEvent).isCancelled();
        }
        return true;
    }

    /**
     * Fire the WiredStackExecutedEvent for plugin compatibility.
     */
    private void fireExecutedEvent(WiredStack stack, WiredEvent event) {
        if (stack.triggerItem() instanceof InteractionWiredTrigger) {
            THashSet<InteractionWiredEffect> legacyEffects = new THashSet<>();
            THashSet<InteractionWiredCondition> legacyConditions = new THashSet<>();
            
            for (IWiredEffect eff : stack.effects()) {
                if (eff instanceof InteractionWiredEffect) {
                    legacyEffects.add((InteractionWiredEffect) eff);
                }
            }
            for (IWiredCondition cond : stack.conditions()) {
                if (cond instanceof InteractionWiredCondition) {
                    legacyConditions.add((InteractionWiredCondition) cond);
                }
            }
            
            Emulator.getPluginManager().fireEvent(new WiredStackExecutedEvent(
                    event.getRoom(),
                    event.getActor().orElse(null),
                    (InteractionWiredTrigger) stack.triggerItem(),
                    legacyEffects,
                    legacyConditions
            ));
        }
    }

    /**
     * Log a debug message if debug mode is enabled.
     */
    private void debug(Room room, String format, Object... args) {
        if (WiredManager.isDebugEnabled()) {
            String message = String.format(format.replace("{}", "%s"), args);
            LOGGER.info("[WiredEngine][Room {}] {}", room.getId(), message);
        }
    }

    /**
     * Activate all extras at the trigger item's location for their animation.
     */
    private void activateExtras(Room room, HabboItem triggerItem, RoomUnit roomUnit, long millis) {
        if (triggerItem == null || room.getRoomSpecialTypes() == null) {
            return;
        }
        
        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(
                triggerItem.getX(), triggerItem.getY());
        
        if (extras != null) {
            for (InteractionWiredExtra extra : extras) {
                extra.activateBox(room, roomUnit, millis);
            }
        }
    }

    /**
     * Get the services used by this engine.
     * @return the wired services
     */
    public WiredServices getServices() {
        return services;
    }

    /**
     * Get the stack index used by this engine.
     * @return the stack index
     */
    public WiredStackIndex getIndex() {
        return index;
    }

    /**
     * Get the maximum steps per stack.
     * @return max steps
     */
    public int getMaxStepsPerStack() {
        return maxStepsPerStack;
    }

    /**
     * Clear all cached unseen indices.
     */
    public void clearUnseenCache() {
        unseenIndices.clear();
    }
    
    /**
     * Clear recursion tracking for a specific room.
     * Should be called when a room is unloaded.
     * @param roomId the room ID
     */
    public void clearRoomRecursionDepth(int roomId) {
        roomRecursionDepth.remove(roomId);
    }
    
    /**
     * Clear all recursion tracking.
     */
    public void clearAllRecursionDepth() {
        roomRecursionDepth.clear();
    }
    
    /**
     * Get the current recursion depth for a room (for debugging).
     * @param roomId the room ID
     * @return the current recursion depth, or 0 if not tracked
     */
    public int getRecursionDepth(int roomId) {
        return roomRecursionDepth.getOrDefault(roomId, 0);
    }
    
    /**
     * Clear rate limiters for a specific room.
     * Should be called when a room is unloaded.
     * @param roomId the room ID
     */
    public void clearRoomRateLimiters(int roomId) {
        String prefix = roomId + ":";
        eventRateLimiters.keySet().removeIf(key -> key.startsWith(prefix));
    }
    
    /**
     * Clear room ban for a specific room.
     * Should be called when a room is unloaded.
     * @param roomId the room ID
     */
    public void clearRoomBan(int roomId) {
        bannedRooms.remove(roomId);
    }
    
    /**
     * Check if a room is currently banned from wired execution.
     * @param roomId the room ID
     * @return true if wired is banned in this room
     */
    private boolean isRoomBanned(int roomId) {
        Long banExpiry = bannedRooms.get(roomId);
        if (banExpiry == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= banExpiry) {
            // Ban expired, remove it
            bannedRooms.remove(roomId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Ban wired execution in a room for WIRED_BAN_DURATION_MS.
     * Sends alerts to all users in the room and a scripter alert to staff.
     * @param roomId the room ID
     * @param room the room object (for sending alerts)
     */
    private void banRoom(int roomId, Room room) {
        long banExpiry = System.currentTimeMillis() + WIRED_BAN_DURATION_MS;
        bannedRooms.put(roomId, banExpiry);
        
        long banMinutes = WIRED_BAN_DURATION_MS / 60000;
        
        // Send alert to all users in the room
        String roomAlertMessage = Emulator.getTexts().getValue("wired.abuse.room.alert")
                .replace("%minutes%", String.valueOf(banMinutes));
        room.sendComposer(new GenericAlertComposer(roomAlertMessage).compose());
        
        // Send scripter bubble alert to staff with room link
        THashMap<String, String> keys = new THashMap<>();
        keys.put("title", Emulator.getTexts().getValue("wired.abuse.staff.title"));
        keys.put("message", Emulator.getTexts().getValue("wired.abuse.staff.message")
                .replace("%roomname%", room.getName())
                .replace("%owner%", room.getOwnerName())
                .replace("%minutes%", String.valueOf(banMinutes)));
        keys.put("linkUrl", "event:navigator/goto/" + roomId);
        keys.put("linkTitle", Emulator.getTexts().getValue("wired.abuse.staff.link"));
        Emulator.getGameEnvironment().getHabboManager().sendPacketToHabbosWithPermission(
                new BubbleAlertComposer("admin.staffalert", keys).compose(), 
                "acc_modtool_room_info"
        );
        
        LOGGER.warn("Wired abuse detected in room {} ({}). Owner: {}. Wired banned for {} minutes.",
                roomId, room.getName(), room.getOwnerName(), banMinutes);
    }
    
    /**
     * Check if an event should be rate-limited.
     * If rate limit exceeded, bans the room and sends alerts.
     * @param roomId the room ID
     * @param room the room object (for sending alerts if banned)
     * @param eventType the event type
     * @return true if the event should be blocked due to rate limiting
     */
    private boolean isRateLimited(int roomId, Room room, WiredEvent.Type eventType) {
        String key = roomId + ":" + eventType.name();
        long now = System.currentTimeMillis();
        
        EventRateTracker tracker = eventRateLimiters.compute(key, (k, existing) -> {
            if (existing == null) {
                return new EventRateTracker(now);
            }
            existing.recordEvent(now);
            return existing;
        });
        
        boolean limited = tracker.isRateLimited(now);
        if (limited && tracker.shouldBan(now)) {
            // First time hitting limit in this suppression window - ban the room
            banRoom(roomId, room);
        }
        return limited;
    }
    
    /**
     * Tracks event rate for a specific room + event type combination.
     */
    private static final class EventRateTracker {
        private long windowStart;
        private int eventCount;
        private boolean banned;
        
        EventRateTracker(long now) {
            this.windowStart = now;
            this.eventCount = 1;
            this.banned = false;
        }
        
        synchronized void recordEvent(long now) {
            // Reset window if expired
            if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
                windowStart = now;
                eventCount = 1;
                // Don't reset banned here - room ban is checked separately
            } else {
                eventCount++;
            }
        }
        
        synchronized boolean isRateLimited(long now) {
            return eventCount > MAX_EVENTS_PER_WINDOW;
        }
        
        /**
         * Check if this is the first time we've hit the limit (to trigger ban).
         * Returns true only once per suppression window.
         */
        synchronized boolean shouldBan(long now) {
            if (eventCount > MAX_EVENTS_PER_WINDOW && !banned) {
                banned = true;
                return true;
            }
            return false;
        }
    }
}
