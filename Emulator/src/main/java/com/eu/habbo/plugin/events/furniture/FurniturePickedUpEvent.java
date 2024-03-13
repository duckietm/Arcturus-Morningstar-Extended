package com.eu.habbo.plugin.events.furniture;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class FurniturePickedUpEvent extends FurnitureUserEvent {

    public FurniturePickedUpEvent(HabboItem furniture, Habbo habbo) {
        super(furniture, habbo);
    }
}
