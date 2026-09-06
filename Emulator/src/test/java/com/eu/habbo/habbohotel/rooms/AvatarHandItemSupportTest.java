package com.eu.habbo.habbohotel.rooms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AvatarHandItemSupportTest {
    @Test
    void acceptsExplicitAndDefaultCarryItemIdsWithinTheRendererRange() {
        assertTrue(AvatarHandItemSupport.supportedCount() > 250);
        assertTrue(AvatarHandItemSupport.isSupported(1));
        assertTrue(AvatarHandItemSupport.isSupported(1129));
        assertTrue(AvatarHandItemSupport.isSupported(2));
        assertTrue(AvatarHandItemSupport.isSupported(5));
        assertTrue(AvatarHandItemSupport.isSupported(6));
        assertTrue(AvatarHandItemSupport.isSupported(7));
        assertTrue(AvatarHandItemSupport.isSupported(389));
        assertTrue(AvatarHandItemSupport.isSupported(1020));
        assertFalse(AvatarHandItemSupport.isSupported(999999999));
        assertEquals(0, AvatarHandItemSupport.normalize(-1));
        assertEquals(5000, AvatarHandItemSupport.normalize(5000));
        assertEquals(0, AvatarHandItemSupport.normalize(5001));
    }
}
