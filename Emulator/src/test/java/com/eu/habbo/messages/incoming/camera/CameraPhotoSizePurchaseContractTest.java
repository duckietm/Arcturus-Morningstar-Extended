package com.eu.habbo.messages.incoming.camera;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraPhotoSizePurchaseContractTest {

    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    @Test
    void purchasePacketSelectsTheConfiguredSmallOrLargeWallPhoto() throws Exception {
        String source = source("src/main/java/com/eu/habbo/messages/incoming/camera/CameraPurchaseEvent.java");

        assertTrue(source.contains("camera.item_id.small"), "Small purchases must use the configured small wall item");
        assertTrue(source.contains("camera.item_id.large"), "Large purchases must use the configured large wall item");
        assertTrue(source.contains("item.getType() != FurnitureType.WALL"), "Camera purchases must reject non-wall items");
    }

    @Test
    void migrationRestoresBothPhotoSizesAsWallItems() throws Exception {
        String migration = source("src/main/resources/db/migration/V20260810184000__camera_photo_size_choices.sql");

        assertTrue(migration.contains("'camera.item_id.small', '45970'"));
        assertTrue(migration.contains("'camera.item_id.large', '45810'"));
        assertTrue(migration.contains("WHERE id IN (45810, 45970)"));
        assertTrue(migration.contains("SET type = 'i', interaction_type = 'external_image'"));
    }
}
