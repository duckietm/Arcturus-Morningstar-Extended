-- The imported 10000-10047 effect libraries are invalid in the deployed Nitro assets.
DELETE FROM `users_effects` WHERE `effect` BETWEEN 10000 AND 10047;
DELETE FROM `special_enables` WHERE `effect_id` BETWEEN 10000 AND 10047;

-- Preserve the existing staff-only effect policy explicitly.
INSERT INTO `special_enables` (`effect_id`, `min_rank`) VALUES
    (102, 5),
    (136, 5),
    (178, 5)
ON DUPLICATE KEY UPDATE `min_rank` = VALUES(`min_rank`);
