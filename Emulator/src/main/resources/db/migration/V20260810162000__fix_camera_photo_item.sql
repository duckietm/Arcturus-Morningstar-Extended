-- The camera must create the actual wall photo, not the unrelated floor poster.
UPDATE emulator_settings
SET value = '45410'
WHERE `key` = 'camera.item_id';
