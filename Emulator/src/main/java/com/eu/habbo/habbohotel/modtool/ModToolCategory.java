package com.eu.habbo.habbohotel.modtool;

import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ModToolCategory {
    private final String name;
    private final TIntObjectMap<ModToolPreset> presets;

    public ModToolCategory(String name) {
        this.name = name;
        this.presets = TCollections.synchronizedMap(new TIntObjectHashMap<>());
    }

    public void addPreset(ModToolPreset preset) {
        this.presets.put(preset.id, preset);
    }

    public TIntObjectMap<ModToolPreset> getPresets() {
        return this.presets;
    }

    public String getName() {
        return this.name;
    }
}
