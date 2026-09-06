-- Every alias handled by TrashCommand and FunRoomCommand shares cmd_trash.
-- Staff starts at rank 4 (Support) in this hotel; Member/VIP/X cannot invoke it.
UPDATE `permission_definitions`
SET `rank_1` = 0,
    `rank_2` = 0,
    `rank_3` = 0,
    `rank_4` = 1,
    `rank_5` = 1,
    `rank_6` = 1,
    `rank_7` = 1,
    `comment` = 'Staff-only fun room events: tornadoes, sharks, cannons, atmospheres and avatar interactions.'
WHERE `permission_key` = 'cmd_trash';

UPDATE `permissions`
SET `cmd_trash` = CASE WHEN `level` >= 4 THEN '1' ELSE '0' END;
