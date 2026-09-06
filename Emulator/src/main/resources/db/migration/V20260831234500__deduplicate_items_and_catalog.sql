-- Consolidate only true base-item duplicates: same classname and same Nitro
-- sprite id. Same classname with a different sprite id is a distinct asset and
-- must not be deleted.
CREATE TEMPORARY TABLE `_items_base_dedupe` (
    `duplicate_id` INT UNSIGNED PRIMARY KEY,
    `canonical_id` INT UNSIGNED NOT NULL,
    KEY `canonical_id` (`canonical_id`)
);

INSERT INTO `_items_base_dedupe` (`duplicate_id`, `canonical_id`)
SELECT ranked.id, ranked.canonical_id
FROM (
    SELECT ib.id,
           FIRST_VALUE(ib.id) OVER (
               PARTITION BY LOWER(ib.item_name), ib.sprite_id
               ORDER BY (ib.id = ib.sprite_id) DESC,
                        (LOWER(ib.type) IN ('s', 'i')) DESC,
                        (ib.interaction_type <> 'default') DESC,
                        ib.id
           ) AS canonical_id,
           COUNT(*) OVER (PARTITION BY LOWER(ib.item_name), ib.sprite_id) AS copies
    FROM items_base ib
    WHERE ib.item_name <> ''
) ranked
WHERE ranked.copies > 1 AND ranked.id <> ranked.canonical_id;

-- Entity/history tables can be updated in place because their own primary key
-- is independent from item_id.
UPDATE `items` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE IGNORE `builders_club_items` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE `calendar_rewards` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE IGNORE `crafting_recipes_ingredients` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE `gift_wrappers` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE `items_crackable` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE `items_hoppers` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.base_item SET t.base_item = m.canonical_id;
UPDATE `items_presents` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.base_item_reward SET t.base_item_reward = m.canonical_id;
UPDATE IGNORE `pet_drinks` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE IGNORE `pet_foods` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE IGNORE `pet_items` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE `recycler_prizes` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;
UPDATE `website_rare_values` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id SET t.item_id = m.canonical_id;

-- If a junction already had the canonical pair, UPDATE IGNORE leaves only the
-- now-redundant duplicate pair behind; remove that pair before deleting the
-- duplicate base definition.
DELETE t FROM `builders_club_items` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id;
DELETE t FROM `crafting_recipes_ingredients` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id;
DELETE t FROM `pet_drinks` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id;
DELETE t FROM `pet_foods` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id;
DELETE t FROM `pet_items` t JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id;

-- Junction table has a foreign key and may already contain the canonical pair.
UPDATE IGNORE `room_templates_items` t
JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id
SET t.item_id = m.canonical_id;
DELETE t FROM `room_templates_items` t
JOIN `_items_base_dedupe` m ON m.duplicate_id = t.item_id;

-- Catalog item_ids is a string because bundles can contain several ids. The
-- equality form safely rewrites ordinary one-item offers without corrupting a
-- bundle expression.
UPDATE `catalog_items` ci
JOIN `_items_base_dedupe` m
  ON ci.item_ids REGEXP '^[0-9]+$'
 AND m.duplicate_id = CONVERT(ci.item_ids, UNSIGNED INTEGER)
SET ci.item_ids = CAST(m.canonical_id AS CHAR);

DELETE ib FROM `items_base` ib
JOIN `_items_base_dedupe` m ON m.duplicate_id = ib.id;

DROP TEMPORARY TABLE `_items_base_dedupe`;

-- This legacy custom row is the same 1x1 Stack Magic asset under a second
-- sprite id. Unlike general same-classname rows, it is a confirmed duplicate.
UPDATE `items` SET `item_id` = 5103 WHERE `item_id` = 6660229;
UPDATE `catalog_items`
SET `item_ids` = '5103'
WHERE `item_ids` = '6660229';
DELETE FROM `items_base` WHERE `id` = 6660229;

-- Remove byte-for-byte equivalent offers from the same page. Preserve limited
-- sale history by pointing it at the lowest (canonical) catalog row first.
CREATE TEMPORARY TABLE `_catalog_item_dedupe` (
    `duplicate_id` INT PRIMARY KEY,
    `canonical_id` INT NOT NULL
);

INSERT INTO `_catalog_item_dedupe` (`duplicate_id`, `canonical_id`)
SELECT ranked.id, ranked.canonical_id
FROM (
    SELECT ci.id,
           MIN(ci.id) OVER (
               PARTITION BY ci.page_id, ci.item_ids, ci.catalog_name,
                            ci.cost_credits, ci.cost_points, ci.points_type,
                            ci.amount, ci.limited_stack, ci.song_id,
                            ci.extradata, ci.club_only
           ) AS canonical_id,
           COUNT(*) OVER (
               PARTITION BY ci.page_id, ci.item_ids, ci.catalog_name,
                            ci.cost_credits, ci.cost_points, ci.points_type,
                            ci.amount, ci.limited_stack, ci.song_id,
                            ci.extradata, ci.club_only
           ) AS copies
    FROM catalog_items ci
) ranked
WHERE ranked.copies > 1 AND ranked.id <> ranked.canonical_id;

UPDATE IGNORE `catalog_items_limited` l
JOIN `_catalog_item_dedupe` m ON m.duplicate_id = l.catalog_item_id
SET l.catalog_item_id = m.canonical_id;
DELETE l FROM `catalog_items_limited` l
JOIN `_catalog_item_dedupe` m ON m.duplicate_id = l.catalog_item_id;
DELETE ci FROM `catalog_items` ci
JOIN `_catalog_item_dedupe` m ON m.duplicate_id = ci.id;

DROP TEMPORARY TABLE `_catalog_item_dedupe`;

-- offer_id must be unique for deterministic catalog navigation. Keep the first
-- occurrence and allocate fresh ids to later, otherwise distinct offers/pages
-- would be lost merely because an imported source reused an id.
SET @next_offer_id := (SELECT GREATEST(COALESCE(MAX(offer_id), 0), 2000000000) FROM catalog_items);
UPDATE catalog_items ci
JOIN (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY offer_id ORDER BY id) AS occurrence
    FROM catalog_items
    WHERE offer_id > 0
) duplicates ON duplicates.id = ci.id
SET ci.offer_id = (@next_offer_id := @next_offer_id + 1)
WHERE duplicates.occurrence > 1;
