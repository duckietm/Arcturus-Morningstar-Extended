ALTER TABLE `users`
  DROP KEY IF EXISTS `auth_ticket`,
  MODIFY `auth_ticket` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  ADD KEY IF NOT EXISTS `idx_users_auth_ticket` (`auth_ticket`);

  ADD KEY IF NOT EXISTS `idx_rel_user_room` (`user_id`, `room_id`);

ALTER TABLE `logs_hc_payday`
  ADD KEY IF NOT EXISTS `idx_lhcp_user_claimed` (`user_id`, `claimed`);

ALTER TABLE `room_votes`
  DROP KEY IF EXISTS `user_id`,
  ADD UNIQUE KEY IF NOT EXISTS `uniq_room_votes_user_room` (`user_id`, `room_id`);

ALTER TABLE `room_game_scores`
  ADD KEY IF NOT EXISTS `idx_rgs_room_ts` (`room_id`, `game_start_timestamp`),
  ADD KEY IF NOT EXISTS `idx_rgs_user`    (`user_id`);

ALTER TABLE `calendar_rewards_claimed`
  DROP KEY IF EXISTS `idx_cal_claimed_user_id`,
  ADD UNIQUE KEY IF NOT EXISTS `uniq_crc_user_campaign_reward` (`user_id`, `campaign_id`, `reward_id`);

ALTER TABLE `emulator_settings`
  ENGINE = InnoDB,
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `gift_wrappers`     ENGINE = InnoDB;
ALTER TABLE `pet_actions`       ENGINE = InnoDB;
ALTER TABLE `pet_commands_data` ENGINE = InnoDB;

ALTER TABLE `calendar_rewards`
  MODIFY `product_name`      VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  MODIFY `custom_image`      VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  MODIFY `badge`             VARCHAR(25)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  MODIFY `subscription_type` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT '';
