-- The public-page remap must never make the Wired root its own child.
UPDATE `catalog_pages`
SET `parent_id` = -1,
    `min_rank` = 1,
    `visible` = '1',
    `enabled` = '1'
WHERE `id` = 1800;

