UPDATE `permission_ranks`
SET `rank_name` = 'Admin'
WHERE `id` = 7;

UPDATE `permission_definitions`
SET `rank_7` = 1;

-- Keep legacy-schema installations aligned as well. Dynamic permission
-- columns are enforced at runtime by Rank for rank 7.
UPDATE `permissions`
SET `rank_name` = 'Admin'
WHERE `id` = 7;
