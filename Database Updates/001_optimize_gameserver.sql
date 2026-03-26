-- =============================================================================
-- Gameserver Database Optimization Migration
-- =============================================================================
-- This migration optimizes the gameserver tables (not website_* tables).
--
-- IMPORTANT: This script is designed to run on a POPULATED database safely.
-- It uses IF NOT EXISTS / IF EXISTS where possible.
--
-- What it does:
--   1. Converts Aria/MyISAM tables to InnoDB (required for foreign keys)
--   2. Fixes data type mismatches (unsigned/signed) so FKs can be created
--   3. Adds missing primary keys and indexes
--   4. Adds foreign key constraints for referential integrity
--
-- BEFORE RUNNING:
--   - Back up your database!
--   - Run on a test environment first
--   - The script disables FK checks during migration to avoid ordering issues
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET @OLD_SQL_MODE = @@SQL_MODE;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

-- =============================================================================
-- PHASE 1: Convert storage engines to InnoDB, ROW_FORMAT to DYNAMIC
-- =============================================================================
-- Foreign keys require InnoDB. Converting Aria and MyISAM tables.
-- InnoDB does not support ROW_FORMAT=FIXED, so all tables get ROW_FORMAT=DYNAMIC.
-- Note: Aria tables lose PAGE_CHECKSUM (InnoDB has its own checksumming).

-- Core emulator tables
ALTER TABLE IF EXISTS `achievements` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `bot_serves` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `catalog_clothing` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `catalog_club_offers` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `catalog_featured_pages` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `catalog_items_limited` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `chatlogs_private` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `chatlogs_room` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `commandlogs` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `emulator_errors` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;

-- Items & marketplace
ALTER TABLE IF EXISTS `items` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `items_crackable` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `items_hoppers` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `items_presents` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `items_teleports` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `marketplace_items` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;

-- Navigator & rooms
ALTER TABLE IF EXISTS `navigator_publiccats` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `navigator_publics` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `navigator_filter` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `navigator_flatcats` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `nux_gifts` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `rooms` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_bans` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_enter_log` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_game_scores` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_models` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_models_custom` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_mutes` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_promotions` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_rights` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_votes` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `room_wordfilter` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;

-- Pets
ALTER TABLE IF EXISTS `pet_breeding` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `pet_breeding_races` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `pet_breeds` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `pet_drinks` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `pet_foods` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `pet_items` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;

-- Polls
ALTER TABLE IF EXISTS `polls` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `polls_answers` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `polls_questions` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;

-- Users
ALTER TABLE IF EXISTS `users_achievements` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_achievements_queue` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_clothing` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_currency` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_effects` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_favorite_rooms` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_navigator_settings` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_pets` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_recipes` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `user_window_settings` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;

-- Misc
ALTER TABLE IF EXISTS `crafting_altars_recipes` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `crafting_recipes` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `crafting_recipes_ingredients` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `namechange_log` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `recycler_prizes` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `special_enables` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `vouchers` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `wordfilter` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `wired_rewards_given` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `youtube_playlists` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `bots` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;


-- =============================================================================
-- PHASE 2: Fix data type mismatches (unsigned vs signed)
-- =============================================================================
-- Foreign keys require EXACT type matches including signedness.
-- Fix columns where referenced PK and referencing FK differ.

-- 2a. users.id is int(11) SIGNED → fix unsigned user_id columns
ALTER TABLE IF EXISTS `logs_hc_payday`
  MODIFY `user_id` int(11) DEFAULT NULL;

ALTER TABLE IF EXISTS `logs_shop_purchases`
  MODIFY `user_id` int(11) DEFAULT NULL;

ALTER TABLE IF EXISTS `users_subscriptions`
  MODIFY `user_id` int(11) DEFAULT NULL;

-- 2b. items_base.id is int(11) UNSIGNED → fix signed FK columns to match
ALTER TABLE IF EXISTS `items`
  MODIFY `item_id` int(11) unsigned DEFAULT 0;

ALTER TABLE IF EXISTS `economy_furniture`
  MODIFY `items_base_id` int(11) unsigned NOT NULL;

ALTER TABLE IF EXISTS `items_crackable`
  MODIFY `item_id` int(11) unsigned NOT NULL;

-- 2c. guilds_forums_threads.id is int(10) UNSIGNED → fix signed FK columns
ALTER TABLE IF EXISTS `guilds_forums_comments`
  MODIFY `thread_id` int(10) unsigned NOT NULL DEFAULT 0;


-- =============================================================================
-- PHASE 3: Add missing primary keys
-- =============================================================================
-- Tables without primary keys hurt performance and replication.

-- bot_serves: no PK
ALTER TABLE IF EXISTS `bot_serves`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- chatlogs_room: no PK (high-volume log table, add auto-increment PK)
ALTER TABLE IF EXISTS `chatlogs_room`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- commandlogs: no PK
ALTER TABLE IF EXISTS `commandlogs`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- crafting_recipes_ingredients: no PK
ALTER TABLE IF EXISTS `crafting_recipes_ingredients`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- items_hoppers: no PK
ALTER TABLE IF EXISTS `items_hoppers`
  ADD PRIMARY KEY (`item_id`);

-- items_presents: no PK
ALTER TABLE IF EXISTS `items_presents`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- items_teleports: no PK
ALTER TABLE IF EXISTS `items_teleports`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- namechange_log: no PK
ALTER TABLE IF EXISTS `namechange_log`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- navigator_publics: no PK
ALTER TABLE IF EXISTS `navigator_publics`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- pet_breeding: no PK
ALTER TABLE IF EXISTS `pet_breeding`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- pet_breeding_races: no PK
ALTER TABLE IF EXISTS `pet_breeding_races`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- pet_drinks: no PK
ALTER TABLE IF EXISTS `pet_drinks`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- pet_foods: no PK
ALTER TABLE IF EXISTS `pet_foods`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- pet_items: no PK
ALTER TABLE IF EXISTS `pet_items`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- pet_vocals: no PK
ALTER TABLE IF EXISTS `pet_vocals`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- recycler_prizes: no PK
ALTER TABLE IF EXISTS `recycler_prizes`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_bans: no PK
ALTER TABLE IF EXISTS `room_bans`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_enter_log: no PK
ALTER TABLE IF EXISTS `room_enter_log`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_game_scores: no PK
ALTER TABLE IF EXISTS `room_game_scores`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_mutes: no PK
ALTER TABLE IF EXISTS `room_mutes`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_rights: no PK (use composite)
ALTER TABLE IF EXISTS `room_rights`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_trax: no PK
ALTER TABLE IF EXISTS `room_trax`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_trax_playlist: no PK
ALTER TABLE IF EXISTS `room_trax_playlist`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- room_votes: no PK (use composite unique)
ALTER TABLE IF EXISTS `room_votes`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- trax_playlist: no PK
ALTER TABLE IF EXISTS `trax_playlist`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- wired_rewards_given: no PK
ALTER TABLE IF EXISTS `wired_rewards_given`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- calendar_rewards_claimed: no PK
ALTER TABLE IF EXISTS `calendar_rewards_claimed`
  ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- camera: no PK
ALTER TABLE IF EXISTS `camera`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,
  ADD PRIMARY KEY (`id`);


-- =============================================================================
-- PHASE 4: Add missing indexes
-- =============================================================================
-- Adding indexes for columns commonly used in JOINs and WHERE clauses.

-- bans: index on user_id for lookups
ALTER TABLE IF EXISTS `bans`
  ADD INDEX `idx_bans_user_id` (`user_id`),
  ADD INDEX `idx_bans_ip` (`ip`),
  ADD INDEX `idx_bans_machine_id` (`machine_id`(64));

-- calendar_rewards: index on campaign_id
ALTER TABLE IF EXISTS `calendar_rewards`
  ADD INDEX `idx_calendar_rewards_campaign_id` (`campaign_id`);

-- calendar_rewards_claimed: indexes for lookups
ALTER TABLE IF EXISTS `calendar_rewards_claimed`
  ADD INDEX `idx_cal_claimed_user_id` (`user_id`),
  ADD INDEX `idx_cal_claimed_campaign_id` (`campaign_id`),
  ADD INDEX `idx_cal_claimed_reward_id` (`reward_id`);

-- camera: indexes
ALTER TABLE IF EXISTS `camera`
  ADD INDEX `idx_camera_user_id` (`user_id`),
  ADD INDEX `idx_camera_room_id` (`room_id`);

-- guilds: index on user_id
ALTER TABLE IF EXISTS `guilds`
  ADD INDEX `idx_guilds_user_id` (`user_id`),
  ADD INDEX `idx_guilds_room_id` (`room_id`);

-- guilds_forums_threads: index on guild_id
ALTER TABLE IF EXISTS `guilds_forums_threads`
  ADD INDEX `idx_gft_guild_id` (`guild_id`),
  ADD INDEX `idx_gft_opener_id` (`opener_id`);

-- guilds_forums_comments: index on user_id
ALTER TABLE IF EXISTS `guilds_forums_comments`
  ADD INDEX `idx_gfc_user_id` (`user_id`);

-- guild_forum_views: indexes
ALTER TABLE IF EXISTS `guild_forum_views`
  ADD INDEX `idx_gfv_user_id` (`user_id`),
  ADD INDEX `idx_gfv_guild_id` (`guild_id`);

-- items_crackable: index on item_id
ALTER TABLE IF EXISTS `items_crackable`
  ADD INDEX `idx_items_crackable_item_id` (`item_id`);

-- namechange_log: index on user_id
ALTER TABLE IF EXISTS `namechange_log`
  ADD INDEX `idx_namechange_user_id` (`user_id`);

-- messenger_friendrequests: indexes
ALTER TABLE IF EXISTS `messenger_friendrequests`
  ADD INDEX `idx_fr_user_to_id` (`user_to_id`),
  ADD INDEX `idx_fr_user_from_id` (`user_from_id`);

-- room_bans: indexes
ALTER TABLE IF EXISTS `room_bans`
  ADD INDEX `idx_room_bans_room_id` (`room_id`),
  ADD INDEX `idx_room_bans_user_id` (`user_id`);

-- room_mutes: indexes
ALTER TABLE IF EXISTS `room_mutes`
  ADD INDEX `idx_room_mutes_room_id` (`room_id`),
  ADD INDEX `idx_room_mutes_user_id` (`user_id`);

-- room_votes: index on room_id
ALTER TABLE IF EXISTS `room_votes`
  ADD INDEX `idx_room_votes_room_id` (`room_id`);

-- sanctions: index on habbo_id
ALTER TABLE IF EXISTS `sanctions`
  ADD INDEX `idx_sanctions_habbo_id` (`habbo_id`);

-- shadowbans: index on user_id
ALTER TABLE IF EXISTS `shadowbans`
  ADD INDEX `idx_shadowbans_user_id` (`user_id`);

-- support_cfh_topics: index on category_id
ALTER TABLE IF EXISTS `support_cfh_topics`
  ADD INDEX `idx_cfh_topics_category_id` (`category_id`);

-- support_tickets: indexes
ALTER TABLE IF EXISTS `support_tickets`
  ADD INDEX `idx_tickets_sender_id` (`sender_id`),
  ADD INDEX `idx_tickets_reported_id` (`reported_id`),
  ADD INDEX `idx_tickets_mod_id` (`mod_id`),
  ADD INDEX `idx_tickets_room_id` (`room_id`);

-- users_settings: already has user_id index, good

-- voucher_history: indexes
ALTER TABLE IF EXISTS `voucher_history`
  ADD INDEX `idx_vh_voucher_id` (`voucher_id`),
  ADD INDEX `idx_vh_user_id` (`user_id`);

-- ls_name_backgrounds_owned
ALTER TABLE IF EXISTS `ls_name_backgrounds_owned`
  ADD INDEX `idx_lsnbo_user_id` (`user_id`),
  ADD INDEX `idx_lsnbo_bg_id` (`name_background_id`);

-- ls_name_colors_owned
ALTER TABLE IF EXISTS `ls_name_colors_owned`
  ADD INDEX `idx_lsnco_user_id` (`user_id`),
  ADD INDEX `idx_lsnco_color_id` (`name_color_id`);

-- ls_prefixes_owned
ALTER TABLE IF EXISTS `ls_prefixes_owned`
  ADD INDEX `idx_lspo_user_id` (`user_id`),
  ADD INDEX `idx_lspo_prefix_id` (`prefix_id`);

-- users_target_offer_purchases
ALTER TABLE IF EXISTS `users_target_offer_purchases`
  ADD INDEX `idx_utop_offer_id` (`offer_id`);

-- users_unlockable_commands
ALTER TABLE IF EXISTS `users_unlockable_commands`
  ADD INDEX `idx_uuc_user_id` (`user_id`);

-- command_category_permissions: index on category_id
ALTER TABLE IF EXISTS `command_category_permissions`
  ADD INDEX `idx_ccp_category_id` (`category_id`);


-- =============================================================================
-- PHASE 5: Foreign key constraints
-- =============================================================================
-- Adding FK constraints for referential integrity.
-- Using appropriate ON DELETE actions:
--   CASCADE  = child rows deleted when parent is deleted
--   SET NULL = child FK set to NULL when parent is deleted (column must be nullable)
--   RESTRICT = prevent parent deletion if children exist (default)
--
-- NOTE: Log/archive tables (chatlogs, commandlogs, room_enter_log, etc.)
-- intentionally do NOT get FKs to avoid cascade-deleting historical data
-- and to keep high-volume inserts fast.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 5.1 Rooms → Users (owner)
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `rooms`
  ADD CONSTRAINT `fk_rooms_owner`
    FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.2 Items → Users, Rooms, Items_base
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `items`
  ADD CONSTRAINT `fk_items_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_items_item_base`
    FOREIGN KEY (`item_id`) REFERENCES `items_base` (`id`)
    ON DELETE CASCADE;

-- Note: items.room_id = 0 means "in inventory", so we can't FK to rooms
-- unless we allow NULL instead of 0. Skipping this FK.

-- ---------------------------------------------------------------------------
-- 5.3 Bots → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `bots`
  ADD CONSTRAINT `fk_bots_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.4 Bans → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `bans`
  ADD CONSTRAINT `fk_bans_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_bans_staff`
    FOREIGN KEY (`user_staff_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.5 Guilds → Users, Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `guilds`
  ADD CONSTRAINT `fk_guilds_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_guilds_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.6 Guild members → Guilds, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `guilds_members`
  ADD CONSTRAINT `fk_gm_guild`
    FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_gm_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.7 Guild forum threads → Guilds, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `guilds_forums_threads`
  ADD CONSTRAINT `fk_gft_guild`
    FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_gft_opener`
    FOREIGN KEY (`opener_id`) REFERENCES `users` (`id`)
    ON DELETE SET NULL;

-- ---------------------------------------------------------------------------
-- 5.8 Guild forum comments → Threads, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `guilds_forums_comments`
  ADD CONSTRAINT `fk_gfc_thread`
    FOREIGN KEY (`thread_id`) REFERENCES `guilds_forums_threads` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_gfc_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.9 Guild forum views → Guilds, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `guild_forum_views`
  ADD CONSTRAINT `fk_gfv_guild`
    FOREIGN KEY (`guild_id`) REFERENCES `guilds` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_gfv_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.10 Catalog items → Catalog pages
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `catalog_items`
  ADD CONSTRAINT `fk_catitems_page`
    FOREIGN KEY (`page_id`) REFERENCES `catalog_pages` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.11 Catalog items limited → Catalog items
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `catalog_items_limited`
  ADD CONSTRAINT `fk_catitemsltd_catitem`
    FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_items` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.12 Calendar rewards → Calendar campaigns
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `calendar_rewards`
  ADD CONSTRAINT `fk_calrewards_campaign`
    FOREIGN KEY (`campaign_id`) REFERENCES `calendar_campaigns` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.13 Calendar rewards claimed → Users, Campaigns, Rewards
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `calendar_rewards_claimed`
  ADD CONSTRAINT `fk_calclaimed_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_calclaimed_campaign`
    FOREIGN KEY (`campaign_id`) REFERENCES `calendar_campaigns` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_calclaimed_reward`
    FOREIGN KEY (`reward_id`) REFERENCES `calendar_rewards` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.14 Users_settings → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_settings`
  ADD CONSTRAINT `fk_usettings_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.15 Users_badges → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_badges`
  ADD CONSTRAINT `fk_ubadges_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.16 Users_currency → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_currency`
  ADD CONSTRAINT `fk_ucurrency_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.17 Users_effects → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_effects`
  ADD CONSTRAINT `fk_ueffects_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.18 Users_clothing → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_clothing`
  ADD CONSTRAINT `fk_uclothing_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.19 Users_favorite_rooms → Users, Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_favorite_rooms`
  ADD CONSTRAINT `fk_ufavrooms_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_ufavrooms_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.20 Users_wardrobe → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_wardrobe`
  ADD CONSTRAINT `fk_uwardrobe_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.21 Users_pets → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_pets`
  ADD CONSTRAINT `fk_upets_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.22 Users_recipes → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_recipes`
  ADD CONSTRAINT `fk_urecipes_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.23 Users_saved_searches → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_saved_searches`
  ADD CONSTRAINT `fk_usavedsearches_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.24 Users_navigator_settings → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_navigator_settings`
  ADD CONSTRAINT `fk_unavsettings_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.25 Users_achievements → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_achievements`
  ADD CONSTRAINT `fk_uachievements_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.26 Users_achievements_queue → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_achievements_queue`
  ADD CONSTRAINT `fk_uachqueue_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.27 Users_ignored → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_ignored`
  ADD CONSTRAINT `fk_uignored_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_uignored_target`
    FOREIGN KEY (`target_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.28 Users_subscriptions → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_subscriptions`
  ADD CONSTRAINT `fk_usubs_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.29 Users_target_offer_purchases → Users, Catalog target offers
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_target_offer_purchases`
  ADD CONSTRAINT `fk_utop_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_utop_offer`
    FOREIGN KEY (`offer_id`) REFERENCES `catalog_target_offers` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.30 Users_unlockable_commands → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `users_unlockable_commands`
  ADD CONSTRAINT `fk_uunlockable_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.31 User_window_settings → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `user_window_settings`
  ADD CONSTRAINT `fk_uwinsettings_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.32 User_prefixes → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `user_prefixes`
  ADD CONSTRAINT `fk_uprefixes_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.33 Messenger_friendships → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `messenger_friendships`
  ADD CONSTRAINT `fk_mfriends_user_one`
    FOREIGN KEY (`user_one_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_mfriends_user_two`
    FOREIGN KEY (`user_two_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.34 Messenger_friendrequests → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `messenger_friendrequests`
  ADD CONSTRAINT `fk_mfr_user_to`
    FOREIGN KEY (`user_to_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_mfr_user_from`
    FOREIGN KEY (`user_from_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.35 Messenger_categories → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `messenger_categories`
  ADD CONSTRAINT `fk_mcat_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.36 Marketplace_items → Items, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `marketplace_items`
  ADD CONSTRAINT `fk_market_item`
    FOREIGN KEY (`item_id`) REFERENCES `items` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_market_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.37 Room_rights → Rooms, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_rights`
  ADD CONSTRAINT `fk_rrights_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_rrights_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.38 Room_bans → Rooms, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_bans`
  ADD CONSTRAINT `fk_rbans_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_rbans_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.39 Room_mutes → Rooms, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_mutes`
  ADD CONSTRAINT `fk_rmutes_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_rmutes_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.40 Room_votes → Rooms, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_votes`
  ADD CONSTRAINT `fk_rvotes_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_rvotes_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.41 Room_wordfilter → Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_wordfilter`
  ADD CONSTRAINT `fk_rwf_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.42 Room_promotions → Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_promotions`
  ADD CONSTRAINT `fk_rpromo_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.43 Room_trax → Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_trax`
  ADD CONSTRAINT `fk_rtrax_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.44 Room_trax_playlist → Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_trax_playlist`
  ADD CONSTRAINT `fk_rtraxpl_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.45 Rooms_for_sale → Rooms, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `rooms_for_sale`
  ADD CONSTRAINT `fk_r4sale_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_r4sale_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.46 Room_trade_log → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_trade_log`
  ADD CONSTRAINT `fk_rtlog_user_one`
    FOREIGN KEY (`user_one_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_rtlog_user_two`
    FOREIGN KEY (`user_two_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.47 Room_trade_log_items → Room_trade_log, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `room_trade_log_items`
  ADD CONSTRAINT `fk_rtli_trade`
    FOREIGN KEY (`id`) REFERENCES `room_trade_log` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_rtli_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.48 Polls_questions → Polls
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `polls_questions`
  ADD CONSTRAINT `fk_pq_poll`
    FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.49 Polls_answers → Polls, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `polls_answers`
  ADD CONSTRAINT `fk_pa_poll`
    FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_pa_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.50 Crafting_recipes_ingredients → Crafting_recipes, Items_base
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `crafting_recipes_ingredients`
  ADD CONSTRAINT `fk_cri_recipe`
    FOREIGN KEY (`recipe_id`) REFERENCES `crafting_recipes` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.51 Crafting_altars_recipes → Crafting_recipes
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `crafting_altars_recipes`
  ADD CONSTRAINT `fk_car_recipe`
    FOREIGN KEY (`recipe_id`) REFERENCES `crafting_recipes` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.52 Voucher_history → Vouchers, Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `voucher_history`
  ADD CONSTRAINT `fk_vh_voucher`
    FOREIGN KEY (`voucher_id`) REFERENCES `vouchers` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_vh_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.53 Support_cfh_topics → Support_cfh_categories
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `support_cfh_topics`
  ADD CONSTRAINT `fk_cfhtopics_category`
    FOREIGN KEY (`category_id`) REFERENCES `support_cfh_categories` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.54 Command_category_permissions → Command_categories
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `command_category_permissions`
  ADD CONSTRAINT `fk_ccp_category`
    FOREIGN KEY (`category_id`) REFERENCES `command_categories` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.55 Economy_furniture → Items_base, Economy_categories
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `economy_furniture`
  ADD CONSTRAINT `fk_econfurni_itembase`
    FOREIGN KEY (`items_base_id`) REFERENCES `items_base` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_econfurni_category`
    FOREIGN KEY (`economy_categories_id`) REFERENCES `economy_categories` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.56 Sanctions → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `sanctions`
  ADD CONSTRAINT `fk_sanctions_user`
    FOREIGN KEY (`habbo_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.57 Camera_web → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `camera_web`
  ADD CONSTRAINT `fk_cameraweb_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.58 LS ownership tables → Users, LS definition tables
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `ls_name_backgrounds_owned`
  ADD CONSTRAINT `fk_lsnbo_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_lsnbo_bg`
    FOREIGN KEY (`name_background_id`) REFERENCES `ls_name_backgrounds` (`id`)
    ON DELETE CASCADE;

ALTER TABLE IF EXISTS `ls_name_colors_owned`
  ADD CONSTRAINT `fk_lsnco_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_lsnco_color`
    FOREIGN KEY (`name_color_id`) REFERENCES `ls_name_colors` (`id`)
    ON DELETE CASCADE;

ALTER TABLE IF EXISTS `ls_prefixes_owned`
  ADD CONSTRAINT `fk_lspo_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_lspo_prefix`
    FOREIGN KEY (`prefix_id`) REFERENCES `ls_prefixes` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.59 Navigator_publics → Navigator_publiccats, Rooms
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `navigator_publics`
  ADD CONSTRAINT `fk_navpub_cat`
    FOREIGN KEY (`public_cat_id`) REFERENCES `navigator_publiccats` (`id`)
    ON DELETE CASCADE,
  ADD CONSTRAINT `fk_navpub_room`
    FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
    ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- 5.60 GOTW winners → Users
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS `gotw_winners`
  ADD CONSTRAINT `fk_gotw_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE;


-- =============================================================================
-- PHASE 6: Charset standardization
-- =============================================================================
-- Standardize remaining utf8mb3 tables to utf8mb4 for full Unicode support.

ALTER TABLE IF EXISTS `guilds`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `guilds_elements`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `groups_items`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `messenger_friendships`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `room_rights`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `soundtracks`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `users_achievements_queue`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `users_saved_searches`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `users_target_offer_purchases`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `wordfilter`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE IF EXISTS `logs_shop_purchases`
  CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- =============================================================================
-- Done - Re-enable foreign key checks
-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;
SET SQL_MODE = @OLD_SQL_MODE;
