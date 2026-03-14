package com.eu.habbo.plugin.events.furniture;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class FurnitureBuildheightEvent extends FurnitureUserEvent {

    public final double oldHeight;
    public final double newHeight;

    private double updatedHeight;

    private boolean changedHeight = false;

    public FurnitureBuildheightEvent(HabboItem furniture, Habbo habbo, double oldHeight, double newHeight) {
        super(furniture, habbo);

        this.oldHeight = oldHeight;
        this.newHeight = newHeight;
    }

    public void setNewHeight(double updatedHeight) {
        this.updatedHeight = updatedHeight;
        this.changedHeight = true;
    }

    public boolean hasChangedHeight() { return changedHeight; }

    public double getUpdatedHeight() { return updatedHeight; }
}
