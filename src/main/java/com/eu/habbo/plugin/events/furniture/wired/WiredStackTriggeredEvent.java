package com.eu.habbo.plugin.events.furniture.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.plugin.events.roomunit.RoomUnitEvent;
import gnu.trove.set.hash.THashSet;

public class WiredStackTriggeredEvent extends RoomUnitEvent {

    public final InteractionWiredTrigger trigger;


    public final THashSet<InteractionWiredEffect> effects;


    public final THashSet<InteractionWiredCondition> conditions;


    public WiredStackTriggeredEvent(Room room, RoomUnit roomUnit, InteractionWiredTrigger trigger, THashSet<InteractionWiredEffect> effects, THashSet<InteractionWiredCondition> conditions) {
        super(room, roomUnit);

        this.trigger = trigger;
        this.effects = effects;
        this.conditions = conditions;
    }
}
