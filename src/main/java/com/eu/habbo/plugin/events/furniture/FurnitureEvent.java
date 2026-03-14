package com.eu.habbo.plugin.events.furniture;

import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.plugin.Event;

public abstract class FurnitureEvent extends Event {

    public final HabboItem furniture;


    public FurnitureEvent(HabboItem furniture) {
        this.furniture = furniture;
    }
}
