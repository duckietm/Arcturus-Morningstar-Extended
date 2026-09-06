ALTER TABLE `permissions`
    ADD COLUMN IF NOT EXISTS `cmd_set_home` enum('0','1') NOT NULL DEFAULT '0',
    ADD COLUMN IF NOT EXISTS `cmd_online` enum('0','1') NOT NULL DEFAULT '0';

UPDATE `permissions`
SET `cmd_set_home` = CASE WHEN `level` >= 7 THEN '1' ELSE '0' END,
    `cmd_online` = '1';

INSERT INTO `permission_definitions`
    (`permission_key`, `comment`, `max_value`, `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('cmd_set_home', 'Allows administrators to set the global home room for every account.', 1, 0, 0, 0, 0, 0, 0, 1),
    ('cmd_online', 'Allows listing online users and their current location.', 1, 1, 1, 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
    `comment` = VALUES(`comment`),
    `max_value` = VALUES(`max_value`),
    `rank_1` = VALUES(`rank_1`),
    `rank_2` = VALUES(`rank_2`),
    `rank_3` = VALUES(`rank_3`),
    `rank_4` = VALUES(`rank_4`),
    `rank_5` = VALUES(`rank_5`),
    `rank_6` = VALUES(`rank_6`),
    `rank_7` = VALUES(`rank_7`);

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_set_home', 'sethome;set_home;impostacasa'),
    ('commands.description.cmd_set_home', ':sethome [roomId] - Imposta la stanza corrente o indicata come home di tutti'),
    ('commands.keys.cmd_online', 'online;on;utenti;connessi'),
    ('commands.description.cmd_online', ':online - Mostra utenti online, rank e stanza corrente')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
