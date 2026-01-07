package com.eu.habbo.habbohotel.items.interactions.wired.interfaces;

import com.eu.habbo.habbohotel.wired.WiredMatchFurniSetting;
import gnu.trove.set.hash.THashSet;

public interface InteractionWiredMatchFurniSettings {
    public THashSet<WiredMatchFurniSetting> getMatchFurniSettings();
    public boolean shouldMatchState();
    public boolean shouldMatchRotation();
    public boolean shouldMatchPosition();
}
