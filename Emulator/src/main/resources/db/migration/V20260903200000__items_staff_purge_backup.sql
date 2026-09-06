-- Staff inventory wipes (":pulisci <utente> si") copy every deleted row here first, so a mistaken purge can be
-- undone: INSERT INTO items (id, user_id, room_id, item_id, wall_pos, x, y, z, rot, extra_data, wired_data,
-- limited_data, guild_id) SELECT id, user_id, room_id, item_id, wall_pos, x, y, z, rot, extra_data, wired_data,
-- limited_data, guild_id FROM items_staff_purge_backup WHERE purge_id = <the id reported in the chat>;
CREATE TABLE IF NOT EXISTS `items_staff_purge_backup` (
    `backup_id` bigint(20) NOT NULL AUTO_INCREMENT,
    `purge_id` bigint(20) NOT NULL COMMENT 'one id per :pulisci run, reported to the staff member',
    `purged_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `purged_by` int(11) NOT NULL,
    `id` int(11) NOT NULL,
    `user_id` int(11) NOT NULL,
    `room_id` int(11) NOT NULL DEFAULT 0,
    `item_id` int(11) unsigned DEFAULT 0,
    `wall_pos` varchar(20) NOT NULL DEFAULT '',
    `x` int(11) NOT NULL DEFAULT 0,
    `y` int(11) NOT NULL DEFAULT 0,
    `z` double(10,6) NOT NULL DEFAULT 0.000000,
    `rot` int(11) NOT NULL DEFAULT 0,
    `extra_data` varchar(1024) NOT NULL DEFAULT '',
    `wired_data` varchar(10000) NOT NULL DEFAULT '',
    `limited_data` varchar(10) NOT NULL DEFAULT '0:0',
    `guild_id` int(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`backup_id`),
    KEY `purge` (`purge_id`),
    KEY `purge_user` (`user_id`, `purged_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
