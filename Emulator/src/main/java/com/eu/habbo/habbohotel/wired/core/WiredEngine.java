package com.eu.habbo.habbohotel.wired.core;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredExtra;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterFurni;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraFilterUser;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraExecutionLimit;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraOrEval;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraRandom;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredExtraUnseen;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.triggers.WiredTriggerHabboClicksUser;
import com.eu.habbo.habbohotel.items.interactions.wired.triggers.WiredTriggerHabboSaysKeyword;
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

    /** Cache room+eventType+sourceItemId -> matching stacks for source-triggered timer events */
    private final ConcurrentHashMap<String, List<WiredStack>> sourceStacksByTriggerKey;

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
        this.sourceStacksByTriggerKey = new ConcurrentHashMap<>();
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

        // Soft rate limiting to prevent rapid-fire event spam without banning whole rooms
        if (isRateLimited(roomId, room, event.getType())) {
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
     * Handle a wired event when the source trigger item is already known.
     * This is mainly used by timed wired triggers to avoid scanning unrelated stacks.
     *
     * @param event the event to handle
     * @param sourceItemId the trigger item id that originated the event
     * @return true if any matching stack was triggered
     */
    public boolean handleEventForSourceItem(WiredEvent event, int sourceItemId) {
        if (event == null || sourceItemId <= 0) {
            return false;
        }

        Room room = event.getRoom();
        if (room == null || !room.isLoaded()) {
            return false;
        }

        int roomId = room.getId();

        if (isRateLimited(roomId, room, event.getType())) {
            return false;
        }

        int currentDepth = roomRecursionDepth.getOrDefault(roomId, 0);
        if (currentDepth >= MAX_RECURSION_DEPTH) {
            LOGGER.warn("Wired recursion limit reached in room {} (depth: {}). " +
                    "Possible infinite loop detected (source item execution). Aborting.", roomId, currentDepth);
            debug(room, "RECURSION LIMIT REACHED - aborting source-item execution");
            return false;
        }
        roomRecursionDepth.put(roomId, currentDepth + 1);

        try {
            return handleEventForSourceItemInternal(event, room, sourceItemId);
        } finally {
            int newDepth = roomRecursionDepth.getOrDefault(roomId, 1) - 1;
            if (newDepth <= 0) {
                roomRecursionDepth.remove(roomId);
            } else {
                roomRecursionDepth.put(roomId, newDepth);
            }
        }
    }

    /**
     * Internal event handling optimized for a known source trigger item.
     */
    private boolean handleEventForSourceItemInternal(WiredEvent event, Room room, int sourceItemId) {
        List<WiredStack> stacks = getStacksForSourceItem(room, event.getType(), sourceItemId);
        if (stacks.isEmpty()) {
            return false;
        }

        debug(room, "Processing {} stacks for event type {} from source item {}", stacks.size(), event.getType(), sourceItemId);

        boolean anyTriggered = false;
        boolean suppressSaysOutput = false;
        long triggerTime = event.getCreatedAtMs();

        for (WiredStack stack : stacks) {
            try {
                boolean triggered = processStack(stack, event, triggerTime);
                if (triggered) {
                    anyTriggered = true;

                    if ((event.getType() == WiredEvent.Type.USER_SAYS)
                            && (stack.triggerItem() instanceof WiredTriggerHabboSaysKeyword)
                            && ((WiredTriggerHabboSaysKeyword) stack.triggerItem()).isHideMessage()) {
                        suppressSaysOutput = true;
                    }
                }
            } catch (WiredLimitException limitEx) {
                debug(room, "Stack execution stopped (limit): {}", limitEx.getMessage());
            } catch (Exception ex) {
                LOGGER.error("Error processing source wired stack in room {} for item {}: {}",
                        room.getId(), sourceItemId, ex.getMessage(), ex);
                debug(room, "Source stack error: {}", ex.getMessage());
            }
        }

        if (event.getType() == WiredEvent.Type.USER_SAYS) {
            return suppressSaysOutput;
        }

        return anyTriggered;
    }

    /**
     * Find all stacks for a specific room/event/source item combination.
     * Multiple stacks can legally share the same trigger item.
     */
    private List<WiredStack> getStacksForSourceItem(Room room, WiredEvent.Type eventType, int sourceItemId) {
        String cacheKey = room.getId() + ":" + eventType.name() + ":" + sourceItemId;

        List<WiredStack> cached = sourceStacksByTriggerKey.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<WiredStack> allStacks = index.getStacks(room, eventType);
        if (allStacks.isEmpty()) {
            sourceStacksByTriggerKey.put(cacheKey, Collections.emptyList());
            return Collections.emptyList();
        }

        List<WiredStack> matching = new ArrayList<>();
        for (WiredStack stack : allStacks) {
            if (stack == null || stack.triggerItem() == null) {
                continue;
            }

            if (stack.triggerItem().getId() == sourceItemId) {
                matching.add(stack);
            }
        }

        List<WiredStack> result = matching.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(matching);
        sourceStacksByTriggerKey.put(cacheKey, result);
        return result;
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
        boolean suppressSaysOutput = false;
        long triggerTime = event.getCreatedAtMs();

        for (WiredStack stack : stacks) {
            try {
                boolean triggered = processStack(stack, event, triggerTime);
                if (triggered) {
                    anyTriggered = true;

                    if ((event.getType() == WiredEvent.Type.USER_SAYS)
                            && (stack.triggerItem() instanceof WiredTriggerHabboSaysKeyword)
                            && ((WiredTriggerHabboSaysKeyword) stack.triggerItem()).isHideMessage()) {
                        suppressSaysOutput = true;
                    }
                }
            } catch (WiredLimitException limitEx) {
                debug(room, "Stack execution stopped (limit): {}", limitEx.getMessage());
            } catch (Exception ex) {
                LOGGER.error("Error processing wired stack in room {}: {}", room.getId(), ex.getMessage(), ex);
                debug(room, "Stack error: {}", ex.getMessage());
            }
        }

        if (event.getType() == WiredEvent.Type.USER_SAYS) {
            return suppressSaysOutput;
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

        debug(room, "Trigger matched: {} at item {} (conditions: {}, effects: {})",
                event.getType(),
                stack.triggerItem() != null ? stack.triggerItem().getId() : "null",
                stack.conditions().size(),
                stack.effects().size());

        // Run selectors before conditions so targets are available
        List<InteractionWiredEffect> executedSelectors = Collections.emptyList();
        if (stack.hasEffects()) {
            executedSelectors = executeSelectors(stack, ctx);
            applySelectionFilterExtras(stack, ctx, executedSelectors);
        }

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

        WiredExtraExecutionLimit executionLimitExtra = getExecutionLimitExtra(room, stack);
        if (executionLimitExtra != null && !executionLimitExtra.tryAcquireExecutionSlot(currentTime)) {
            debug(room, "Execution limit blocked stack {} (max {} in {} ms)",
                    stack.triggerItem() != null ? stack.triggerItem().getId() : "null",
                    executionLimitExtra.getMaxExecutions(),
                    executionLimitExtra.getTimeWindowMs());
            return false;
        }

        // Fire plugin event (WiredStackTriggeredEvent)
        if (!fireTriggeredEvent(stack, event)) {
            debug(room, "Stack cancelled by plugin");
            return false;
        }

        if ((event.getType() == WiredEvent.Type.USER_CLICKS_USER)
                && (stack.triggerItem() instanceof WiredTriggerHabboClicksUser)
                && event.getActor().isPresent()) {
            WiredTriggerHabboClicksUser clickUserTrigger = (WiredTriggerHabboClicksUser) stack.triggerItem();
            WiredTriggerHabboClicksUser.applyRuntimeOptions(
                    event.getActor().get(),
                    clickUserTrigger.isBlockMenuOpen(),
                    clickUserTrigger.isDoNotRotate());
        }

        RoomUnit actor = event.getActor().orElse(null);

        // Only show the trigger/selector activation when the stack is actually allowed to continue.
        if (stack.triggerItem() instanceof InteractionWiredTrigger) {
            InteractionWiredTrigger trigger = (InteractionWiredTrigger) stack.triggerItem();
            trigger.activateBox(room, actor, currentTime);
        }

        activateExtras(room, stack.triggerItem(), actor, currentTime);
        finalizeSelectors(executedSelectors, ctx, currentTime);

        // Execute effects
        if (stack.hasEffects()) {
            executeEffects(stack, ctx, currentTime);
        }

        // Fire executed event
        fireExecutedEvent(stack, event);

        return true;
    }

    private boolean wouldTriggerStack(WiredStack stack, WiredEvent event, long currentTime) {
        Room room = event.getRoom();

        if (!stack.trigger().matches(stack.triggerItem(), event)) {
            return false;
        }

        if (stack.trigger().requiresActor() && !event.getActor().isPresent()) {
            return false;
        }

        WiredState state = new WiredState(maxStepsPerStack);
        WiredContext ctx = new WiredContext(event, stack.triggerItem(), stack, services, state, null);

        state.step();

        List<InteractionWiredEffect> executedSelectors = Collections.emptyList();
        if (stack.hasEffects()) {
            executedSelectors = executeSelectors(stack, ctx);
            applySelectionFilterExtras(stack, ctx, executedSelectors);
        }

        if (stack.hasConditions() && !evaluateConditions(stack, ctx)) {
            return false;
        }

        WiredExtraExecutionLimit executionLimitExtra = getExecutionLimitExtra(room, stack);
        return executionLimitExtra == null || executionLimitExtra.canExecuteAt(currentTime);
    }

    /**
     * Evaluate all conditions in a stack.
     */
    private boolean evaluateConditions(WiredStack stack, WiredContext ctx) {
        List<IWiredCondition> conditions = stack.conditions();

        return evaluateConditionsByMode(conditions, ctx, stack.conditionEvaluationMode(), stack.conditionEvaluationValue());
    }

    /**
     * Evaluate conditions according to the configured stack mode.
     */
    private boolean evaluateConditionsByMode(List<IWiredCondition> conditions, WiredContext ctx, int evaluationMode, int evaluationValue) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        Room room = ctx.room();
        Map<String, List<Boolean>> groupedOrResults = new LinkedHashMap<>();
        int matchedRequirements = 0;
        int totalRequirements = 0;

        for (IWiredCondition condition : conditions) {
            ctx.state().step();

            boolean result = condition.evaluate(ctx);
            String conditionKey = getConditionGroupKey(condition);

            if (condition.operator() == WiredConditionOperator.OR) {
                groupedOrResults.computeIfAbsent(conditionKey, ignored -> new ArrayList<>()).add(result);
                debug(room, "  Condition (OR group {}) {}: {}", conditionKey, condition.getClass().getSimpleName(), result ? "PASS" : "FAIL");
                continue;
            }

            totalRequirements++;

            if (result) {
                matchedRequirements++;
            }

            debug(room, "  Condition {}: {}", condition.getClass().getSimpleName(), result ? "PASS" : "FAIL");
        }

        for (Map.Entry<String, List<Boolean>> entry : groupedOrResults.entrySet()) {
            totalRequirements++;

            boolean groupPassed = entry.getValue().stream().anyMatch(Boolean::booleanValue);
            if (groupPassed) {
                matchedRequirements++;
            }

            debug(room, "  Condition (OR result {}) : {}", entry.getKey(), groupPassed ? "PASS" : "FAIL");
        }

        boolean matches = WiredExtraOrEval.matchesMode(evaluationMode, matchedRequirements, totalRequirements, evaluationValue);

        debug(room, "Condition eval mode {} value {} matched {}/{} logical requirements => {}", evaluationMode, evaluationValue, matchedRequirements, totalRequirements, matches ? "PASS" : "FAIL");
        return matches;
    }

    private String getConditionGroupKey(IWiredCondition condition) {
        if (condition instanceof InteractionWiredCondition) {
            return String.valueOf(((InteractionWiredCondition) condition).getType());
        }

        return condition.getClass().getName();
    }

    /**
     * Execute effects in a stack.
     */
    private void executeEffects(WiredStack stack, WiredContext ctx, long currentTime) {
        List<IWiredEffect> effects = stack.effects();

        if (effects.isEmpty()) {
            return;
        }

        // Selectors already executed before conditions; only run regular effects here
        List<IWiredEffect> regulars = new ArrayList<>();
        for (IWiredEffect e : effects) {
            if (!e.isSelector()) regulars.add(e);
        }

        // Determine which (regular) effects to execute
        List<IWiredEffect> toExecute;

        if (stack.useRandom()) {
            WiredExtraRandom randomExtra = getRandomExtra(ctx.room(), stack);
            if (regulars.isEmpty()) {
                toExecute = new ArrayList<>();
            } else if (randomExtra != null) {
                toExecute = randomExtra.selectWiredEffects(regulars);
                debug(ctx.room(), "Random mode: selected {} effect(s), skip window {}", toExecute.size(), randomExtra.getSkipExecutions());
            } else {
                int randomIndex = new Random().nextInt(regulars.size());
                toExecute = Collections.singletonList(regulars.get(randomIndex));
                debug(ctx.room(), "Random mode: selected effect {}/{}", randomIndex + 1, regulars.size());
            }
        } else if (stack.useUnseen()) {
            // Unseen mode: execute in stable order with memory
            if (regulars.isEmpty()) {
                toExecute = new ArrayList<>();
            } else {
                WiredExtraUnseen unseenExtra = getUnseenExtra(ctx.room(), stack);

                if (unseenExtra != null) {
                    toExecute = unseenExtra.selectWiredEffects(regulars);

                    if (!toExecute.isEmpty()) {
                        int selectedIndex = regulars.indexOf(toExecute.get(0));
                        debug(ctx.room(), "Unseen mode: selected effect {}/{}", selectedIndex + 1, regulars.size());
                    } else {
                        debug(ctx.room(), "Unseen mode: no eligible effect found");
                    }
                } else {
                    int index = getNextUnseenIndex(stack, regulars.size());
                    toExecute = Collections.singletonList(regulars.get(index));
                    debug(ctx.room(), "Unseen mode fallback: selected effect {}/{}", index + 1, regulars.size());
                }
            }
        } else if (stack.executeInOrder()) {
            debug(ctx.room(), "Ordered mode: executing effect batches in stack order by delay");
            executeOrderedEffects(regulars, ctx, currentTime);
            return;
        } else {
            // Normal mode: regular effects in random order
            toExecute = new ArrayList<>(regulars);
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
     * Execute selector effects before conditions so ctx.targets() is populated.
     */
    private List<InteractionWiredEffect> executeSelectors(WiredStack stack, WiredContext ctx) {
        List<IWiredEffect> effects = stack.effects();
        if (effects.isEmpty()) return Collections.emptyList();

        List<InteractionWiredEffect> executedSelectors = new ArrayList<>();

        for (IWiredEffect effect : effects) {
            if (!effect.isSelector()) continue;
            if (effect.requiresActor() && !ctx.hasActor()) {
                continue;
            }

            ctx.state().step();
            try {
                effect.execute(ctx);
                if (effect instanceof InteractionWiredEffect) {
                    executedSelectors.add((InteractionWiredEffect) effect);
                }
            } catch (Exception e) {
                LOGGER.warn("Error executing selector: {}", e.getMessage());
            }
        }

        return executedSelectors;
    }

    private void finalizeSelectors(List<InteractionWiredEffect> executedSelectors, WiredContext ctx, long currentTime) {
        if (executedSelectors == null || executedSelectors.isEmpty()) {
            return;
        }

        Room room = ctx.room();
        RoomUnit actor = ctx.actor().orElse(null);

        for (InteractionWiredEffect wiredEffect : executedSelectors) {
            wiredEffect.setCooldown(currentTime);
            wiredEffect.activateBox(room, actor, currentTime);
        }
    }

    private void applySelectionFilterExtras(WiredStack stack, WiredContext ctx, List<InteractionWiredEffect> executedSelectors) {
        if (executedSelectors == null || executedSelectors.isEmpty()) {
            return;
        }

        Room room = ctx.room();
        if (room == null || stack.triggerItem() == null || room.getRoomSpecialTypes() == null) {
            return;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(
                stack.triggerItem().getX(),
                stack.triggerItem().getY());

        if (extras == null || extras.isEmpty()) {
            return;
        }

        int furniLimit = Integer.MAX_VALUE;
        int userLimit = Integer.MAX_VALUE;

        for (InteractionWiredExtra extra : extras) {
            if (extra instanceof WiredExtraFilterFurni) {
                furniLimit = Math.min(furniLimit, ((WiredExtraFilterFurni) extra).getAmount());
            } else if (extra instanceof WiredExtraFilterUser) {
                userLimit = Math.min(userLimit, ((WiredExtraFilterUser) extra).getAmount());
            }
        }

        if (ctx.targets().isItemsModifiedBySelector() && furniLimit != Integer.MAX_VALUE) {
            ctx.targets().setItems(limitIterable(ctx.targets().items(), furniLimit));
        }

        if (ctx.targets().isUsersModifiedBySelector() && userLimit != Integer.MAX_VALUE) {
            ctx.targets().setUsers(limitIterable(ctx.targets().users(), userLimit));
        }
    }

    private <T> List<T> limitIterable(Iterable<T> values, int limit) {
        List<T> result = new ArrayList<>();

        if (values == null || limit <= 0) {
            return result;
        }

        for (T value : values) {
            if (value != null) {
                result.add(value);
            }
        }

        if (result.size() <= limit) {
            return result;
        }

        Collections.shuffle(result, Emulator.getRandom());
        return new ArrayList<>(result.subList(0, limit));
    }

    /**
     * Schedule a delayed effect execution.
     */
    private void scheduleDelayedEffect(IWiredEffect effect, WiredContext ctx, int delay, long triggerTime) {
        // Delay is in 500ms ticks
        long delayMs = delay * 500L;
        long elapsedSinceTrigger = Math.max(0L, System.currentTimeMillis() - triggerTime);
        long remainingDelayMs = Math.max(0L, delayMs - elapsedSinceTrigger);
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
        }, remainingDelayMs);
    }

    private void executeOrderedEffects(List<IWiredEffect> effects, WiredContext ctx, long currentTime) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        Map<Integer, List<IWiredEffect>> effectsByDelay = new LinkedHashMap<>();

        for (IWiredEffect effect : effects) {
            if (effect == null) {
                continue;
            }

            if (effect.requiresActor() && !ctx.hasActor()) {
                continue;
            }

            effectsByDelay.computeIfAbsent(effect.getDelay(), key -> new ArrayList<>()).add(effect);
        }

        for (Map.Entry<Integer, List<IWiredEffect>> entry : effectsByDelay.entrySet()) {
            int delay = entry.getKey();
            List<IWiredEffect> batch = entry.getValue();

            if (batch.isEmpty()) {
                continue;
            }

            if (delay > 0) {
                scheduleOrderedEffectBatch(batch, ctx, delay, currentTime);
            } else {
                executeOrderedEffectBatch(batch, ctx, currentTime, false);
            }
        }
    }

    /**
     * Preview whether a USER_SAYS event should suppress the public room chat output.
     * This mirrors trigger and condition eligibility without executing regular effects.
     */
    public boolean shouldSuppressUserSaysOutput(WiredEvent event) {
        if (event == null || event.getType() != WiredEvent.Type.USER_SAYS) {
            return false;
        }

        Room room = event.getRoom();
        if (room == null || !room.isLoaded()) {
            return false;
        }

        List<WiredStack> stacks = index.getStacks(room, event.getType());
        if (stacks.isEmpty()) {
            return false;
        }

        long triggerTime = event.getCreatedAtMs();

        for (WiredStack stack : stacks) {
            if (!(stack.triggerItem() instanceof WiredTriggerHabboSaysKeyword)) {
                continue;
            }

            WiredTriggerHabboSaysKeyword trigger = (WiredTriggerHabboSaysKeyword) stack.triggerItem();
            if (!trigger.isHideMessage()) {
                continue;
            }

            try {
                if (wouldTriggerStack(stack, event, triggerTime)) {
                    return true;
                }
            } catch (WiredLimitException limitEx) {
                debug(room, "Suppression preview stopped (limit): {}", limitEx.getMessage());
            } catch (Exception ex) {
                LOGGER.warn("Error previewing USER_SAYS suppression in room {}: {}", room.getId(), ex.getMessage());
            }
        }

        return false;
    }

    private void scheduleOrderedEffectBatch(List<IWiredEffect> batch, WiredContext ctx, int delay, long triggerTime) {
        long delayMs = delay * 500L;
        long elapsedSinceTrigger = Math.max(0L, System.currentTimeMillis() - triggerTime);
        long remainingDelayMs = Math.max(0L, delayMs - elapsedSinceTrigger);
        Room room = ctx.room();

        Emulator.getThreading().run(() -> {
            if (!room.isLoaded() || room.getHabbos().isEmpty()) {
                return;
            }

            executeOrderedEffectBatch(batch, ctx, System.currentTimeMillis(), true);
        }, remainingDelayMs);
    }

    private void executeOrderedEffectBatch(List<IWiredEffect> batch, WiredContext ctx, long executionTime, boolean useExecutionTimeForCooldown) {
        Room room = ctx.room();
        RoomUnit actor = ctx.actor().orElse(null);

        for (IWiredEffect effect : batch) {
            try {
                if (!useExecutionTimeForCooldown) {
                    ctx.state().step();
                }

                effect.execute(ctx);

                if (effect instanceof InteractionWiredEffect) {
                    InteractionWiredEffect wiredEffect = (InteractionWiredEffect) effect;
                    wiredEffect.setCooldown(executionTime);
                    wiredEffect.activateBox(room, actor, executionTime);
                }
            } catch (Exception e) {
                LOGGER.warn("Error executing ordered effect batch item: {}", e.getMessage());
            }
        }
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
        if (!WiredManager.isDebugEnabled()) {
            return;
        }

        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        String message = String.format(format.replace("{}", "%s"), args);
        LOGGER.debug("[WiredEngine][Room {}] {}", room.getId(), message);
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

    private WiredExtraRandom getRandomExtra(Room room, WiredStack stack) {
        InteractionWiredExtra extra = getStackExtra(room, stack, WiredExtraRandom.class);

        return (extra instanceof WiredExtraRandom) ? (WiredExtraRandom) extra : null;
    }

    private WiredExtraUnseen getUnseenExtra(Room room, WiredStack stack) {
        InteractionWiredExtra extra = getStackExtra(room, stack, WiredExtraUnseen.class);

        return (extra instanceof WiredExtraUnseen) ? (WiredExtraUnseen) extra : null;
    }

    private WiredExtraExecutionLimit getExecutionLimitExtra(Room room, WiredStack stack) {
        InteractionWiredExtra extra = getStackExtra(room, stack, WiredExtraExecutionLimit.class);

        return (extra instanceof WiredExtraExecutionLimit) ? (WiredExtraExecutionLimit) extra : null;
    }

    private <T extends InteractionWiredExtra> InteractionWiredExtra getStackExtra(Room room, WiredStack stack, Class<T> extraClass) {
        if (room == null || stack == null || stack.triggerItem() == null || room.getRoomSpecialTypes() == null) {
            return null;
        }

        THashSet<InteractionWiredExtra> extras = room.getRoomSpecialTypes().getExtras(
                stack.triggerItem().getX(),
                stack.triggerItem().getY());

        if (extras == null || extras.isEmpty()) {
            return null;
        }

        for (InteractionWiredExtra extra : extras) {
            if (extraClass.isInstance(extra)) {
                return extra;
            }
        }

        return null;
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
     * Clear cached source-stack lookups for a specific room.
     * @param roomId the room ID
     */
    public void clearRoomSourceStackCache(int roomId) {
        String prefix = roomId + ":";
        sourceStacksByTriggerKey.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Clear all cached source-stack lookups.
     */
    public void clearAllSourceStackCache() {
        sourceStacksByTriggerKey.clear();
    }

    /**
     * Clear all execution-related caches for a specific room.
     * @param roomId the room ID
     */
    public void clearRoomExecutionCaches(int roomId) {
        clearRoomRecursionDepth(roomId);
        clearRoomRateLimiters(roomId);
        clearRoomSourceStackCache(roomId);
    }

    /**
     * Clear all execution-related caches.
     */
    public void clearAllExecutionCaches() {
        clearAllRecursionDepth();
        eventRateLimiters.clear();
        clearAllSourceStackCache();
        clearUnseenCache();
    }

    /**
     * Clear room ban for a specific room.
     * @param roomId the room ID
     */
    public void clearRoomBan(int roomId) {
        // no-op
    }

    /**
     * Check if a room is currently banned from wired execution.
     * @param roomId the room ID
     * @return true if wired is banned in this room
     */
    private boolean isRoomBanned(int roomId) {
        return false;
    }

    /**
     * Ban wired execution in a room.
     * @param roomId the room ID
     * @param room the room object
     */
    private void banRoom(int roomId, Room room) {
        // no-op
    }

    /**
     * Check if an event should be rate-limited.
     * Uses a soft limiter only, without banning rooms.
     * @param roomId the room ID
     * @param room the room object
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
            LOGGER.warn("Soft wired rate limit in room {} for event {}. Count in current window exceeded.",
                    roomId, eventType);
        }
        return limited;
    }

    /**
     * Tracks event rate for a specific room + event type combination.
     */
    private static final class EventRateTracker {
        private long windowStart;
        private int eventCount;
        private boolean warned;

        EventRateTracker(long now) {
            this.windowStart = now;
            this.eventCount = 1;
            this.warned = false;
        }

        synchronized void recordEvent(long now) {
            if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
                windowStart = now;
                eventCount = 1;
                warned = false;
            } else {
                eventCount++;
            }
        }

        synchronized boolean isRateLimited(long now) {
            return eventCount > MAX_EVENTS_PER_WINDOW;
        }

        synchronized boolean shouldBan(long now) {
            if (eventCount > MAX_EVENTS_PER_WINDOW && !warned) {
                warned = true;
                return true;
            }
            return false;
        }
    }
}
