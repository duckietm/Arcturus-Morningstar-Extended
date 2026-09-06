-- Xabbo imports introduced duplicate Stack Magic base rows with wall-item type
-- and 1x1 fallback geometry. Polaris must treat every variant as a floor stack
-- helper, while sprite_id remains the furnidata id sent to Nitro.
UPDATE `items_base`
SET `type` = 's',
    `interaction_type` = 'stack_helper',
    `interaction_modes_count` = 2,
    `allow_stack` = 1,
    `allow_walk` = 1
WHERE LOWER(`item_name`) LIKE 'tile_stackmagic%'
   OR LOWER(`public_name`) LIKE 'tile_stackmagic%';

UPDATE `items_base` SET `width` = 1, `length` = 1
WHERE LOWER(`item_name`) = 'tile_stackmagic';
UPDATE `items_base` SET `width` = 2, `length` = 1
WHERE LOWER(`item_name`) = 'tile_stackmagic1';
UPDATE `items_base` SET `width` = 2, `length` = 2
WHERE LOWER(`item_name`) = 'tile_stackmagic2';
UPDATE `items_base` SET `width` = 10, `length` = 10
WHERE LOWER(`item_name`) = 'tile_stackmagic3';
UPDATE `items_base` SET `width` = 4, `length` = 4
WHERE LOWER(`item_name`) IN ('tile_stackmagic4', 'tile_stackmagic4x4', 'tile_stackmagic9');
UPDATE `items_base` SET `width` = 1, `length` = 1
WHERE LOWER(`item_name`) = 'tile_stackmagic5';
UPDATE `items_base` SET `width` = 6, `length` = 4
WHERE LOWER(`item_name`) = 'tile_stackmagic6';
UPDATE `items_base` SET `width` = 6, `length` = 6
WHERE LOWER(`item_name`) = 'tile_stackmagic6x6';
UPDATE `items_base` SET `width` = 8, `length` = 8
WHERE LOWER(`item_name`) = 'tile_stackmagic8x8';
