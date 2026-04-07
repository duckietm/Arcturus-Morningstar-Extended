-- Sample seed for Builders Club catalog pages/items
-- Safe to run multiple times: it recreates the demo BC pages and their demo BC offers.
-- After import, publish/reload the catalog.

SET @bc_demo_root_caption := 'BC Demo Root';
SET @bc_demo_page_caption := 'BC Demo Furni';

DELETE FROM `catalog_items_bc`
WHERE `page_id` IN (
    SELECT `id`
    FROM (
        SELECT `id`
        FROM `catalog_pages_bc`
        WHERE `caption` IN (@bc_demo_root_caption, @bc_demo_page_caption)
    ) AS `bc_pages_to_clear`
);

DELETE FROM `catalog_pages_bc`
WHERE `caption` IN (@bc_demo_root_caption, @bc_demo_page_caption);

INSERT INTO `catalog_pages_bc`
(
    `parent_id`,
    `caption`,
    `page_layout`,
    `icon_color`,
    `icon_image`,
    `order_num`,
    `visible`,
    `enabled`,
    `page_headline`,
    `page_teaser`,
    `page_special`,
    `page_text1`,
    `page_text2`,
    `page_text_details`,
    `page_text_teaser`
)
VALUES
(
    -1,
    @bc_demo_root_caption,
    'default_3x3',
    1,
    28,
    999,
    '1',
    '1',
    '',
    '',
    '',
    'Builders Club Demo',
    'Pagina demo creata da seed SQL',
    'Root demo per il catalogo Builders Club.',
    ''
);

SET @bc_demo_root_id := LAST_INSERT_ID();

INSERT INTO `catalog_pages_bc`
(
    `parent_id`,
    `caption`,
    `page_layout`,
    `icon_color`,
    `icon_image`,
    `order_num`,
    `visible`,
    `enabled`,
    `page_headline`,
    `page_teaser`,
    `page_special`,
    `page_text1`,
    `page_text2`,
    `page_text_details`,
    `page_text_teaser`
)
VALUES
(
    @bc_demo_root_id,
    @bc_demo_page_caption,
    'default_3x3',
    1,
    28,
    1,
    '1',
    '1',
    '',
    '',
    '',
    'Builders Club Furni',
    'Furni demo',
    'Questa pagina duplica alcuni furni del catalogo normale dentro al Builders Club.',
    ''
);

SET @bc_demo_page_id := LAST_INSERT_ID();

-- Source page from normal catalog: tries to use the "base" furni line.
SET @source_normal_page_id := (
    SELECT `id`
    FROM `catalog_pages`
    WHERE `caption_save` = 'base'
    ORDER BY `id`
    LIMIT 1
);

-- Copy only safe single-placeable demo furni:
-- - one single numeric item id
-- - floor furni only
-- - size 1x1
-- - default interaction only
-- This avoids copying bundles, bots, effects, teleports, wall items and large furni that can fail placement during BC testing.
INSERT INTO `catalog_items_bc`
(
    `item_ids`,
    `page_id`,
    `catalog_name`,
    `order_number`,
    `extradata`
)
SELECT
    `ci`.`item_ids`,
    @bc_demo_page_id,
    CONCAT(`ci`.`catalog_name`, '_bc_demo'),
    `ci`.`order_number`,
    `ci`.`extradata`
FROM `catalog_items` `ci`
INNER JOIN `items_base` `ib`
    ON `ib`.`id` = CAST(`ci`.`item_ids` AS UNSIGNED)
WHERE `ci`.`page_id` = @source_normal_page_id
  AND `ci`.`item_ids` REGEXP '^[0-9]+$'
  AND `ib`.`type` = 's'
  AND `ib`.`width` = 1
  AND `ib`.`length` = 1
  AND `ib`.`interaction_type` = 'default'
ORDER BY `ci`.`order_number`, `ci`.`id`
LIMIT 6;

-- Fallback: if page "base" is missing or empty, duplicate any 6 safe 1x1 floor offers.
INSERT INTO `catalog_items_bc`
(
    `item_ids`,
    `page_id`,
    `catalog_name`,
    `order_number`,
    `extradata`
)
SELECT
    `fallback_ci`.`item_ids`,
    @bc_demo_page_id,
    CONCAT(`fallback_ci`.`catalog_name`, '_bc_demo'),
    `fallback_ci`.`id`,
    `fallback_ci`.`extradata`
FROM `catalog_items` `fallback_ci`
INNER JOIN `items_base` `fallback_ib`
    ON `fallback_ib`.`id` = CAST(`fallback_ci`.`item_ids` AS UNSIGNED)
WHERE NOT EXISTS (
    SELECT 1
    FROM `catalog_items_bc`
    WHERE `page_id` = @bc_demo_page_id
)
  AND `fallback_ci`.`item_ids` REGEXP '^[0-9]+$'
  AND `fallback_ib`.`type` = 's'
  AND `fallback_ib`.`width` = 1
  AND `fallback_ib`.`length` = 1
  AND `fallback_ib`.`interaction_type` = 'default'
ORDER BY `fallback_ci`.`id`
LIMIT 6;

SELECT @bc_demo_root_id AS `bc_root_page_id`, @bc_demo_page_id AS `bc_furni_page_id`;
