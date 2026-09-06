package com.eu.habbo.habbohotel.items.interactions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InteractionExternalImageTest {
    @Test
    void rewritesLegacyLoopbackPhotoToSameOrigin() {
        String input = "{\"t\":1,\"w\":\"http://127.0.0.1:8080/camera/1_1786371173.png\"}";
        assertEquals("{\"t\":1,\"w\":\"/camera/1_1786371173.png\"}", InteractionExternalImage.normalizePhotoExtradata(input));
    }

    @Test
    void rewritesImportedBssPhotoToSameOrigin() {
        String input = "{\"w\":\"https://photo.bsshotel.it//1_1786371173.png\"}";
        assertEquals("{\"w\":\"/camera/1_1786371173.png\"}", InteractionExternalImage.normalizePhotoExtradata(input));
    }

    @Test
    void preservesUnrelatedExternalImages() {
        String input = "{\"w\":\"https://example.com/poster.png\"}";
        assertEquals(input, InteractionExternalImage.normalizePhotoExtradata(input));
    }
}
