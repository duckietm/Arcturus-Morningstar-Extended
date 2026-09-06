package com.eu.habbo.habbohotel.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AvatarEffectSupportTest {
    @Test
    void acceptsOnlyEffectsBackedByTheDeployedEffectMapAndBundleSet() {
        assertTrue(AvatarEffectSupport.supportedCount() > 500);
        assertTrue(AvatarEffectSupport.isSupported(1));
        assertTrue(AvatarEffectSupport.isSupported(4000));
        assertFalse(AvatarEffectSupport.isSupported(-3));
        assertFalse(AvatarEffectSupport.isSupported(3999));
        assertEquals(0, AvatarEffectSupport.normalize(-3));
        assertEquals(0, AvatarEffectSupport.normalize(999999999));
    }
}
