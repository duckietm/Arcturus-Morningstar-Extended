ALTER TABLE `users` DROP KEY IF EXISTS `auth_ticket`;
ALTER TABLE `users`
  MODIFY `auth_ticket` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';
CREATE INDEX IF NOT EXISTS `idx_users_auth_ticket` ON `users` (`auth_ticket`);
CREATE INDEX IF NOT EXISTS `idx_rel_user_room` ON `room_enter_log` (`user_id`, `room_id`);
CREATE INDEX IF NOT EXISTS `idx_lhcp_user_claimed` ON `logs_hc_payday` (`user_id`, `claimed`);
CREATE UNIQUE INDEX IF NOT EXISTS `uniq_room_votes_user_room` ON `room_votes` (`user_id`, `room_id`);
ALTER TABLE `room_votes` DROP KEY IF EXISTS `user_id`;
CREATE INDEX IF NOT EXISTS `idx_rgs_room_ts` ON `room_game_scores` (`room_id`, `game_start_timestamp`);
CREATE INDEX IF NOT EXISTS `idx_rgs_user`    ON `room_game_scores` (`user_id`);
CREATE UNIQUE INDEX IF NOT EXISTS `uniq_crc_user_campaign_reward`
  ON `calendar_rewards_claimed` (`user_id`, `campaign_id`, `reward_id`);
ALTER TABLE `calendar_rewards_claimed` DROP KEY IF EXISTS `idx_cal_claimed_user_id`;
ALTER TABLE `emulator_settings`
  ENGINE = InnoDB,
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP TABLE IF EXISTS `gift_wrappers_new`;
CREATE TABLE `gift_wrappers_new` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sprite_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `type` enum('gift','wrapper') NOT NULL DEFAULT 'wrapper',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO `gift_wrappers_new` (`id`, `sprite_id`, `item_id`, `type`)
  SELECT `id`, `sprite_id`, `item_id`, `type` FROM `gift_wrappers`;
DROP TABLE `gift_wrappers`;
RENAME TABLE `gift_wrappers_new` TO `gift_wrappers`;


DROP TABLE IF EXISTS `pet_actions_new`;
CREATE TABLE `pet_actions_new` (
  `pet_type` int(2) NOT NULL AUTO_INCREMENT,
  `pet_name` varchar(32) NOT NULL,
  `offspring_type` int(3) NOT NULL DEFAULT -1,
  `happy_actions` varchar(100) NOT NULL DEFAULT '',
  `tired_actions` varchar(100) NOT NULL DEFAULT '',
  `random_actions` varchar(100) NOT NULL DEFAULT '',
  `can_swim` enum('1','0') DEFAULT '0',
  PRIMARY KEY (`pet_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO `pet_actions_new`
    (`pet_type`, `pet_name`, `offspring_type`, `happy_actions`, `tired_actions`, `random_actions`, `can_swim`)
  SELECT `pet_type`, `pet_name`, `offspring_type`, `happy_actions`, `tired_actions`, `random_actions`, `can_swim`
  FROM `pet_actions`;
DROP TABLE `pet_actions`;
RENAME TABLE `pet_actions_new` TO `pet_actions`;


DROP TABLE IF EXISTS `pet_commands_data_new`;
CREATE TABLE `pet_commands_data_new` (
  `command_id` int(3) NOT NULL,
  `text` varchar(25) NOT NULL,
  `required_level` int(2) NOT NULL,
  `reward_xp` int(3) NOT NULL DEFAULT 5,
  `cost_happiness` int(11) NOT NULL DEFAULT 0,
  `cost_energy` int(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`command_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO `pet_commands_data_new`
    (`command_id`, `text`, `required_level`, `reward_xp`, `cost_happiness`, `cost_energy`)
  SELECT `command_id`, `text`, `required_level`, `reward_xp`, `cost_happiness`, `cost_energy`
  FROM `pet_commands_data`;
DROP TABLE `pet_commands_data`;
RENAME TABLE `pet_commands_data_new` TO `pet_commands_data`;

ALTER TABLE `calendar_rewards`
  MODIFY `product_name`      VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  MODIFY `custom_image`      VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  MODIFY `badge`             VARCHAR(25)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  MODIFY `subscription_type` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT '';
