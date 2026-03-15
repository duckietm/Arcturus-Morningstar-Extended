package com.eu.habbo.habbohotel.wired.api;

import com.eu.habbo.habbohotel.users.HabboItem;

import java.util.Collections;
import java.util.List;

/**
 * Represents a wired stack - a trigger with its associated conditions and effects.
 * <p>
 * In Habbo, wired items stacked on the same tile form a logical unit:
 * <ul>
 *   <li>One trigger at the base (what starts the chain)</li>
 *   <li>Zero or more conditions (requirements that must be met)</li>
 *   <li>One or more effects (actions to perform)</li>
 *   <li>Optional extras (modifiers like random selection)</li>
 * </ul>
 * </p>
 * 
 * <h3>Execution Flow:</h3>
 * <ol>
 *   <li>Trigger receives an event and calls {@link IWiredTrigger#matches}</li>
 *   <li>If matched, all conditions are evaluated via {@link IWiredCondition#evaluate}</li>
 *   <li>If conditions pass, effects execute via {@link IWiredEffect#execute}</li>
 * </ol>
 * 
 * @see IWiredTrigger
 * @see IWiredCondition
 * @see IWiredEffect
 */
public final class WiredStack {
    
    private final HabboItem triggerItem;
    private final IWiredTrigger trigger;
    private final List<IWiredCondition> conditions;
    private final List<IWiredEffect> effects;
    
    // Extra modifiers
    private final boolean useOrMode;       // WiredExtraOrEval present
    private final boolean useRandom;        // WiredExtraRandom present
    private final boolean useUnseen;        // WiredExtraUnseen present

    /**
     * Create a new wired stack.
     * 
     * @param triggerItem the wired trigger furniture item
     * @param trigger the trigger implementation
     * @param conditions list of conditions (may be empty)
     * @param effects list of effects (should have at least one)
     */
    public WiredStack(HabboItem triggerItem,
                      IWiredTrigger trigger,
                      List<IWiredCondition> conditions,
                      List<IWiredEffect> effects) {
        this(triggerItem, trigger, conditions, effects, false, false, false);
    }

    /**
     * Create a new wired stack with modifiers.
     * 
     * @param triggerItem the wired trigger furniture item
     * @param trigger the trigger implementation
     * @param conditions list of conditions
     * @param effects list of effects
     * @param useOrMode if true, conditions use OR logic (any pass = success)
     * @param useRandom if true, select one random effect instead of all
     * @param useUnseen if true, execute effects in "unseen" order (round-robin)
     */
    public WiredStack(HabboItem triggerItem,
                      IWiredTrigger trigger,
                      List<IWiredCondition> conditions,
                      List<IWiredEffect> effects,
                      boolean useOrMode,
                      boolean useRandom,
                      boolean useUnseen) {
        this.triggerItem = triggerItem;
        this.trigger = trigger;
        this.conditions = conditions != null ? Collections.unmodifiableList(conditions) : Collections.emptyList();
        this.effects = effects != null ? Collections.unmodifiableList(effects) : Collections.emptyList();
        this.useOrMode = useOrMode;
        this.useRandom = useRandom;
        this.useUnseen = useUnseen;
    }

    /**
     * Get the wired trigger furniture item.
     * @return the trigger item
     */
    public HabboItem triggerItem() {
        return triggerItem;
    }

    /**
     * Get the trigger implementation.
     * @return the trigger
     */
    public IWiredTrigger trigger() {
        return trigger;
    }

    /**
     * Get the list of conditions.
     * @return unmodifiable list of conditions
     */
    public List<IWiredCondition> conditions() {
        return conditions;
    }

    /**
     * Get the list of effects.
     * @return unmodifiable list of effects
     */
    public List<IWiredEffect> effects() {
        return effects;
    }

    /**
     * Check if this stack has any conditions.
     * @return true if there are conditions
     */
    public boolean hasConditions() {
        return !conditions.isEmpty();
    }

    /**
     * Check if this stack has any effects.
     * @return true if there are effects
     */
    public boolean hasEffects() {
        return !effects.isEmpty();
    }

    /**
     * Check if OR mode is enabled (WiredExtraOrEval).
     * When true, any condition passing means all pass.
     * @return true if OR mode is enabled
     */
    public boolean useOrMode() {
        return useOrMode;
    }

    /**
     * Check if random mode is enabled (WiredExtraRandom).
     * When true, only one random effect is executed.
     * @return true if random mode is enabled
     */
    public boolean useRandom() {
        return useRandom;
    }

    /**
     * Check if unseen mode is enabled (WiredExtraUnseen).
     * When true, effects execute in round-robin order.
     * @return true if unseen mode is enabled
     */
    public boolean useUnseen() {
        return useUnseen;
    }

    /**
     * Get the number of conditions.
     * @return condition count
     */
    public int conditionCount() {
        return conditions.size();
    }

    /**
     * Get the number of effects.
     * @return effect count
     */
    public int effectCount() {
        return effects.size();
    }

    @Override
    public String toString() {
        return "WiredStack{" +
                "triggerItem=" + (triggerItem != null ? triggerItem.getId() : "null") +
                ", trigger=" + (trigger != null ? trigger.listensTo() : "null") +
                ", conditions=" + conditions.size() +
                ", effects=" + effects.size() +
                ", orMode=" + useOrMode +
                ", random=" + useRandom +
                ", unseen=" + useUnseen +
                '}';
    }
}
