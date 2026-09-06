-- Camera checkout offers the two actual wall-photo furni sizes.
INSERT INTO emulator_settings (`key`, value, comment)
VALUES
    ('camera.item_id.small', '45970', 'Base item ID used by the generated small camera wall photo.'),
    ('camera.item_id.large', '45810', 'Base item ID used by the generated large camera wall photo.')
ON DUPLICATE KEY UPDATE comment = VALUES(comment);

-- The BSS furniture import classified these wall-photo assets as floor items.
-- Keep their imported sprite IDs, but restore the inventory/placement category.
UPDATE items_base
SET type = 'i', interaction_type = 'external_image'
WHERE id IN (45810, 45970);
