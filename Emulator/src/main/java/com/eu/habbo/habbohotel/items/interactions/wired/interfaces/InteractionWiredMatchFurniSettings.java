package com.eu.habbo.habbohotel.items.interactions.wired.interfaces;

import com.eu.habbo.habbohotel.wired.WiredMatchFurniSetting;
import gnu.trove.set.hash.THashSet;

public interface InteractionWiredMatchFurniSettings {
    THashSet<WiredMatchFurniSetting> getMatchFurniSettings();
    boolean shouldMatchState();
    boolean shouldMatchRotation();
    boolean shouldMatchPosition();
}
