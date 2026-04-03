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
-- Uses a helper procedure to safely add `id` column only if it doesn't exist.

DELIMITER //
DROP PROCEDURE IF EXISTS `_add_id_pk_if_missing`//
CREATE PROCEDURE `_add_id_pk_if_missing`(IN tbl VARCHAR(64))
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl AND `COLUMN_NAME` = 'id';
    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL `_add_id_pk_if_missing`('bot_serves');
CALL `_add_id_pk_if_missing`('chatlogs_room');
CALL `_add_id_pk_if_missing`('commandlogs');
CALL `_add_id_pk_if_missing`('crafting_recipes_ingredients');

-- items_hoppers: use existing item_id as PK (skip if PK already exists)
SET @has_pk = (SELECT COUNT(*) FROM `information_schema`.`TABLE_CONSTRAINTS`
    WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'items_hoppers' AND `CONSTRAINT_TYPE` = 'PRIMARY KEY');
SET @sql = IF(@has_pk = 0, 'ALTER TABLE `items_hoppers` ADD PRIMARY KEY (`item_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CALL `_add_id_pk_if_missing`('items_presents');
CALL `_add_id_pk_if_missing`('items_teleports');
CALL `_add_id_pk_if_missing`('namechange_log');
CALL `_add_id_pk_if_missing`('navigator_publics');

CALL `_add_id_pk_if_missing`('pet_breeding');
CALL `_add_id_pk_if_missing`('pet_breeding_races');
CALL `_add_id_pk_if_missing`('pet_drinks');
CALL `_add_id_pk_if_missing`('pet_foods');
CALL `_add_id_pk_if_missing`('pet_items');
CALL `_add_id_pk_if_missing`('pet_vocals');
CALL `_add_id_pk_if_missing`('recycler_prizes');
CALL `_add_id_pk_if_missing`('room_bans');
CALL `_add_id_pk_if_missing`('room_enter_log');
CALL `_add_id_pk_if_missing`('room_game_scores');
CALL `_add_id_pk_if_missing`('room_mutes');
CALL `_add_id_pk_if_missing`('room_rights');
CALL `_add_id_pk_if_missing`('room_trax');
CALL `_add_id_pk_if_missing`('room_trax_playlist');
CALL `_add_id_pk_if_missing`('room_votes');
CALL `_add_id_pk_if_missing`('trax_playlist');
CALL `_add_id_pk_if_missing`('wired_rewards_given');

CALL `_add_id_pk_if_missing`('calendar_rewards_claimed');

-- camera: ensure id is auto-increment PK (skip ADD PK if already exists)
SET @has_pk = (SELECT COUNT(*) FROM `information_schema`.`TABLE_CONSTRAINTS`
    WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'camera' AND `CONSTRAINT_TYPE` = 'PRIMARY KEY');
SET @sql = IF(@has_pk = 0,
    'ALTER TABLE `camera` MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, ADD PRIMARY KEY (`id`)',
    'ALTER TABLE `camera` MODIFY `id` int(11) NOT NULL AUTO_INCREMENT');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Clean up helper procedure
DROP PROCEDURE IF EXISTS `_add_id_pk_if_missing`;

-- =============================================================================
-- PHASE 4: Add missing indexes
-- =============================================================================
-- Adding indexes for columns commonly used in JOINs and WHERE clauses.
-- Uses a helper procedure to skip indexes that already exist.

DELIMITER //
DROP PROCEDURE IF EXISTS `_add_index_if_missing`//
CREATE PROCEDURE `_add_index_if_missing`(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(255))
BEGIN
    DECLARE tbl_exists INT DEFAULT 0;
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO tbl_exists
        FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl;
    IF tbl_exists = 0 THEN
        -- Table does not exist, skip
        SET @dummy = 0;
    ELSE
        SELECT COUNT(*) INTO idx_exists
            FROM `information_schema`.`STATISTICS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl AND `INDEX_NAME` = idx;
        IF idx_exists = 0 THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD INDEX `', idx, '` (', cols, ')');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END//
DELIMITER ;

-- bans
CALL `_add_index_if_missing`('bans', 'idx_bans_user_id', '`user_id`');
CALL `_add_index_if_missing`('bans', 'idx_bans_ip', '`ip`');
CALL `_add_index_if_missing`('bans', 'idx_bans_machine_id', '`machine_id`(64)');

-- calendar_rewards
CALL `_add_index_if_missing`('calendar_rewards', 'idx_calendar_rewards_campaign_id', '`campaign_id`');

-- calendar_rewards_claimed
CALL `_add_index_if_missing`('calendar_rewards_claimed', 'idx_cal_claimed_user_id', '`user_id`');
CALL `_add_index_if_missing`('calendar_rewards_claimed', 'idx_cal_claimed_campaign_id', '`campaign_id`');
CALL `_add_index_if_missing`('calendar_rewards_claimed', 'idx_cal_claimed_reward_id', '`reward_id`');

-- camera
CALL `_add_index_if_missing`('camera', 'idx_camera_user_id', '`user_id`');
CALL `_add_index_if_missing`('camera', 'idx_camera_room_id', '`room_id`');

-- guilds
CALL `_add_index_if_missing`('guilds', 'idx_guilds_user_id', '`user_id`');
CALL `_add_index_if_missing`('guilds', 'idx_guilds_room_id', '`room_id`');

-- guilds_forums_threads
CALL `_add_index_if_missing`('guilds_forums_threads', 'idx_gft_guild_id', '`guild_id`');
CALL `_add_index_if_missing`('guilds_forums_threads', 'idx_gft_opener_id', '`opener_id`');

-- guilds_forums_comments
CALL `_add_index_if_missing`('guilds_forums_comments', 'idx_gfc_user_id', '`user_id`');

-- guild_forum_views
CALL `_add_index_if_missing`('guild_forum_views', 'idx_gfv_user_id', '`user_id`');
CALL `_add_index_if_missing`('guild_forum_views', 'idx_gfv_guild_id', '`guild_id`');

-- items_crackable
CALL `_add_index_if_missing`('items_crackable', 'idx_items_crackable_item_id', '`item_id`');

-- namechange_log
CALL `_add_index_if_missing`('namechange_log', 'idx_namechange_user_id', '`user_id`');

-- messenger_friendrequests
CALL `_add_index_if_missing`('messenger_friendrequests', 'idx_fr_user_to_id', '`user_to_id`');
CALL `_add_index_if_missing`('messenger_friendrequests', 'idx_fr_user_from_id', '`user_from_id`');

-- room_bans
CALL `_add_index_if_missing`('room_bans', 'idx_room_bans_room_id', '`room_id`');
CALL `_add_index_if_missing`('room_bans', 'idx_room_bans_user_id', '`user_id`');

-- room_mutes
CALL `_add_index_if_missing`('room_mutes', 'idx_room_mutes_room_id', '`room_id`');
CALL `_add_index_if_missing`('room_mutes', 'idx_room_mutes_user_id', '`user_id`');

-- room_votes
CALL `_add_index_if_missing`('room_votes', 'idx_room_votes_room_id', '`room_id`');

-- sanctions
CALL `_add_index_if_missing`('sanctions', 'idx_sanctions_habbo_id', '`habbo_id`');

-- shadowbans
CALL `_add_index_if_missing`('shadowbans', 'idx_shadowbans_user_id', '`user_id`');

-- support_cfh_topics
CALL `_add_index_if_missing`('support_cfh_topics', 'idx_cfh_topics_category_id', '`category_id`');

-- support_tickets
CALL `_add_index_if_missing`('support_tickets', 'idx_tickets_sender_id', '`sender_id`');
CALL `_add_index_if_missing`('support_tickets', 'idx_tickets_reported_id', '`reported_id`');
CALL `_add_index_if_missing`('support_tickets', 'idx_tickets_mod_id', '`mod_id`');
CALL `_add_index_if_missing`('support_tickets', 'idx_tickets_room_id', '`room_id`');

-- voucher_history
CALL `_add_index_if_missing`('voucher_history', 'idx_vh_voucher_id', '`voucher_id`');
CALL `_add_index_if_missing`('voucher_history', 'idx_vh_user_id', '`user_id`');

-- ls_name_backgrounds_owned
CALL `_add_index_if_missing`('ls_name_backgrounds_owned', 'idx_lsnbo_user_id', '`user_id`');
CALL `_add_index_if_missing`('ls_name_backgrounds_owned', 'idx_lsnbo_bg_id', '`name_background_id`');

-- ls_name_colors_owned
CALL `_add_index_if_missing`('ls_name_colors_owned', 'idx_lsnco_user_id', '`user_id`');
CALL `_add_index_if_missing`('ls_name_colors_owned', 'idx_lsnco_color_id', '`name_color_id`');

-- ls_prefixes_owned
CALL `_add_index_if_missing`('ls_prefixes_owned', 'idx_lspo_user_id', '`user_id`');
CALL `_add_index_if_missing`('ls_prefixes_owned', 'idx_lspo_prefix_id', '`prefix_id`');

-- users_target_offer_purchases
CALL `_add_index_if_missing`('users_target_offer_purchases', 'idx_utop_offer_id', '`offer_id`');

-- users_unlockable_commands
CALL `_add_index_if_missing`('users_unlockable_commands', 'idx_uuc_user_id', '`user_id`');

-- command_category_permissions
CALL `_add_index_if_missing`('command_category_permissions', 'idx_ccp_category_id', '`category_id`');

-- Clean up helper procedure
DROP PROCEDURE IF EXISTS `_add_index_if_missing`;


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
-- 5.0 Clean orphan rows BEFORE adding foreign keys
-- ---------------------------------------------------------------------------
-- Legacy Habbo databases often have rows referencing deleted users, rooms, or
-- items. These orphans must be removed before FK constraints can be created.
-- Uses a helper procedure to safely skip tables that don't exist.

DELIMITER //
DROP PROCEDURE IF EXISTS `_delete_orphans`//
CREATE PROCEDURE `_delete_orphans`(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ref_tbl VARCHAR(64), IN ref_col VARCHAR(64), IN extra_where VARCHAR(255))
BEGIN
    DECLARE tbl_exists INT DEFAULT 0;
    DECLARE ref_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO tbl_exists
        FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl;
    SELECT COUNT(*) INTO ref_exists
        FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = ref_tbl;
    IF tbl_exists > 0 AND ref_exists > 0 THEN
        IF extra_where IS NOT NULL AND extra_where != '' THEN
            SET @sql = CONCAT('DELETE FROM `', tbl, '` WHERE ', extra_where, ' AND `', col, '` NOT IN (SELECT `', ref_col, '` FROM `', ref_tbl, '`)');
        ELSE
            SET @sql = CONCAT('DELETE FROM `', tbl, '` WHERE `', col, '` NOT IN (SELECT `', ref_col, '` FROM `', ref_tbl, '`)');
        END IF;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

-- Orphans referencing users
CALL `_delete_orphans`('rooms', 'owner_id', 'users', 'id', '');
CALL `_delete_orphans`('items', 'user_id', 'users', 'id', '');
-- bots: skipped, no FK added (see 5.3 comment)
CALL `_delete_orphans`('bans', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('bans', 'user_staff_id', 'users', 'id', '');
CALL `_delete_orphans`('guilds', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('guilds_members', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('guilds_forums_comments', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('guild_forum_views', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_settings', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_badges', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_currency', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_effects', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_clothing', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_favorite_rooms', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_wardrobe', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_pets', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_recipes', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_saved_searches', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_navigator_settings', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_achievements', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_achievements_queue', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_ignored', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_ignored', 'target_id', 'users', 'id', '');
CALL `_delete_orphans`('users_subscriptions', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_target_offer_purchases', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_unlockable_outfit', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_window_settings', 'user_id', 'users', 'id', '');
CALL `_delete_orphans`('users_prefixes', 'user_id', 'users', 'id', '');

-- Orphans referencing rooms
CALL `_delete_orphans`('guilds', 'room_id', 'rooms', 'id', '');
CALL `_delete_orphans`('items', 'room_id', 'rooms', 'id', '`room_id` != 0');

-- Orphans referencing items_base
CALL `_delete_orphans`('items', 'item_id', 'items_base', 'id', '');

-- Orphans referencing guilds
CALL `_delete_orphans`('guilds_members', 'guild_id', 'guilds', 'id', '');
CALL `_delete_orphans`('guilds_forums_threads', 'guild_id', 'guilds', 'id', '');
CALL `_delete_orphans`('guild_forum_views', 'guild_id', 'guilds', 'id', '');

-- Clean up helper procedure
DROP PROCEDURE IF EXISTS `_delete_orphans`;

-- ---------------------------------------------------------------------------
-- Helper: add FK only if it doesn't already exist
-- ---------------------------------------------------------------------------
DELIMITER //
DROP PROCEDURE IF EXISTS `_add_fk_if_missing`//
CREATE PROCEDURE `_add_fk_if_missing`(IN tbl VARCHAR(64), IN fk_name VARCHAR(64), IN col VARCHAR(64), IN ref_tbl VARCHAR(64), IN ref_col VARCHAR(64), IN on_del VARCHAR(20))
BEGIN
    DECLARE tbl_exists INT DEFAULT 0;
    DECLARE ref_exists INT DEFAULT 0;
    DECLARE fk_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO tbl_exists
        FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl;
    SELECT COUNT(*) INTO ref_exists
        FROM `information_schema`.`TABLES`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = ref_tbl;
    IF tbl_exists = 0 OR ref_exists = 0 THEN
        -- Source or referenced table does not exist, skip
        SET @dummy = 0;
    ELSE
        SELECT COUNT(*) INTO fk_exists
            FROM `information_schema`.`TABLE_CONSTRAINTS`
            WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl AND `CONSTRAINT_NAME` = fk_name AND `CONSTRAINT_TYPE` = 'FOREIGN KEY';
        IF fk_exists = 0 THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD CONSTRAINT `', fk_name, '` FOREIGN KEY (`', col, '`) REFERENCES `', ref_tbl, '` (`', ref_col, '`) ON DELETE ', on_del);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END//
DELIMITER ;

-- 5.1 Rooms → Users
CALL `_add_fk_if_missing`('rooms', 'fk_rooms_owner', 'owner_id', 'users', 'id', 'CASCADE');

-- 5.2 Items → Users, Items_base
CALL `_add_fk_if_missing`('items', 'fk_items_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('items', 'fk_items_item_base', 'item_id', 'items_base', 'id', 'CASCADE');

-- 5.3 Bots → Users
-- SKIPPED: The emulator creates bots with a temporary user_id during catalog
-- purchase, which may not yet exist in `users`. Drop FK if previously added.
SET @has_fk = (SELECT COUNT(*) FROM `information_schema`.`TABLE_CONSTRAINTS`
    WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'bots' AND `CONSTRAINT_NAME` = 'fk_bots_user' AND `CONSTRAINT_TYPE` = 'FOREIGN KEY');
SET @sql = IF(@has_fk > 0, 'ALTER TABLE `bots` DROP FOREIGN KEY `fk_bots_user`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5.4 Bans → Users
CALL `_add_fk_if_missing`('bans', 'fk_bans_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('bans', 'fk_bans_staff', 'user_staff_id', 'users', 'id', 'CASCADE');

-- 5.5 Guilds → Users, Rooms
CALL `_add_fk_if_missing`('guilds', 'fk_guilds_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('guilds', 'fk_guilds_room', 'room_id', 'rooms', 'id', 'CASCADE');

-- 5.6 Guild members → Guilds, Users
CALL `_add_fk_if_missing`('guilds_members', 'fk_gm_guild', 'guild_id', 'guilds', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('guilds_members', 'fk_gm_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.7 Guild forum threads → Guilds, Users
CALL `_add_fk_if_missing`('guilds_forums_threads', 'fk_gft_guild', 'guild_id', 'guilds', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('guilds_forums_threads', 'fk_gft_opener', 'opener_id', 'users', 'id', 'SET NULL');

-- 5.8 Guild forum comments → Threads, Users
CALL `_add_fk_if_missing`('guilds_forums_comments', 'fk_gfc_thread', 'thread_id', 'guilds_forums_threads', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('guilds_forums_comments', 'fk_gfc_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.9 Guild forum views → Guilds, Users
CALL `_add_fk_if_missing`('guild_forum_views', 'fk_gfv_guild', 'guild_id', 'guilds', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('guild_forum_views', 'fk_gfv_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.10 Catalog items → Catalog pages
CALL `_add_fk_if_missing`('catalog_items', 'fk_catitems_page', 'page_id', 'catalog_pages', 'id', 'CASCADE');

-- 5.11 Catalog items limited → Catalog items
CALL `_add_fk_if_missing`('catalog_items_limited', 'fk_catitemsltd_catitem', 'catalog_item_id', 'catalog_items', 'id', 'CASCADE');

-- 5.12 Calendar rewards → Calendar campaigns
CALL `_add_fk_if_missing`('calendar_rewards', 'fk_calrewards_campaign', 'campaign_id', 'calendar_campaigns', 'id', 'CASCADE');

-- 5.13 Calendar rewards claimed → Users, Campaigns, Rewards
CALL `_add_fk_if_missing`('calendar_rewards_claimed', 'fk_calclaimed_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('calendar_rewards_claimed', 'fk_calclaimed_campaign', 'campaign_id', 'calendar_campaigns', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('calendar_rewards_claimed', 'fk_calclaimed_reward', 'reward_id', 'calendar_rewards', 'id', 'CASCADE');

-- 5.14-5.26 Users_* → Users
CALL `_add_fk_if_missing`('users_settings', 'fk_usettings_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_badges', 'fk_ubadges_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_currency', 'fk_ucurrency_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_effects', 'fk_ueffects_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_clothing', 'fk_uclothing_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_favorite_rooms', 'fk_ufavrooms_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_favorite_rooms', 'fk_ufavrooms_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_wardrobe', 'fk_uwardrobe_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_pets', 'fk_upets_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_recipes', 'fk_urecipes_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_saved_searches', 'fk_usavedsearches_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_navigator_settings', 'fk_unavsettings_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_achievements', 'fk_uachievements_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_achievements_queue', 'fk_uachqueue_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_ignored', 'fk_uignored_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_ignored', 'fk_uignored_target', 'target_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_subscriptions', 'fk_usubs_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_target_offer_purchases', 'fk_utop_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_target_offer_purchases', 'fk_utop_offer', 'offer_id', 'catalog_target_offers', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('users_unlockable_commands', 'fk_uunlockable_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('user_window_settings', 'fk_uwinsettings_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('user_prefixes', 'fk_uprefixes_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.33-5.35 Messenger → Users
CALL `_add_fk_if_missing`('messenger_friendships', 'fk_mfriends_user_one', 'user_one_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('messenger_friendships', 'fk_mfriends_user_two', 'user_two_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('messenger_friendrequests', 'fk_mfr_user_to', 'user_to_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('messenger_friendrequests', 'fk_mfr_user_from', 'user_from_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('messenger_categories', 'fk_mcat_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.36 Marketplace → Items, Users
CALL `_add_fk_if_missing`('marketplace_items', 'fk_market_item', 'item_id', 'items', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('marketplace_items', 'fk_market_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.37-5.44 Room_* → Rooms, Users
CALL `_add_fk_if_missing`('room_rights', 'fk_rrights_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_rights', 'fk_rrights_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_bans', 'fk_rbans_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_bans', 'fk_rbans_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_mutes', 'fk_rmutes_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_mutes', 'fk_rmutes_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_votes', 'fk_rvotes_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_votes', 'fk_rvotes_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_wordfilter', 'fk_rwf_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_promotions', 'fk_rpromo_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_trax', 'fk_rtrax_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_trax_playlist', 'fk_rtraxpl_room', 'room_id', 'rooms', 'id', 'CASCADE');

-- 5.45 Rooms_for_sale → Rooms, Users
CALL `_add_fk_if_missing`('rooms_for_sale', 'fk_r4sale_room', 'room_id', 'rooms', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('rooms_for_sale', 'fk_r4sale_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.46-5.47 Room_trade_log → Users
CALL `_add_fk_if_missing`('room_trade_log', 'fk_rtlog_user_one', 'user_one_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_trade_log', 'fk_rtlog_user_two', 'user_two_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_trade_log_items', 'fk_rtli_trade', 'id', 'room_trade_log', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('room_trade_log_items', 'fk_rtli_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.48-5.49 Polls → Polls, Users
CALL `_add_fk_if_missing`('polls_questions', 'fk_pq_poll', 'poll_id', 'polls', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('polls_answers', 'fk_pa_poll', 'poll_id', 'polls', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('polls_answers', 'fk_pa_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.50-5.51 Crafting → Crafting_recipes
CALL `_add_fk_if_missing`('crafting_recipes_ingredients', 'fk_cri_recipe', 'recipe_id', 'crafting_recipes', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('crafting_altars_recipes', 'fk_car_recipe', 'recipe_id', 'crafting_recipes', 'id', 'CASCADE');

-- 5.52 Voucher_history → Vouchers, Users
CALL `_add_fk_if_missing`('voucher_history', 'fk_vh_voucher', 'voucher_id', 'vouchers', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('voucher_history', 'fk_vh_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.53-5.55 Support, Commands, Economy
CALL `_add_fk_if_missing`('support_cfh_topics', 'fk_cfhtopics_category', 'category_id', 'support_cfh_categories', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('command_category_permissions', 'fk_ccp_category', 'category_id', 'command_categories', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('economy_furniture', 'fk_econfurni_itembase', 'items_base_id', 'items_base', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('economy_furniture', 'fk_econfurni_category', 'economy_categories_id', 'economy_categories', 'id', 'CASCADE');

-- 5.56-5.57 Sanctions, Camera → Users
CALL `_add_fk_if_missing`('sanctions', 'fk_sanctions_user', 'habbo_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('camera_web', 'fk_cameraweb_user', 'user_id', 'users', 'id', 'CASCADE');

-- 5.58 LS ownership → Users, LS definitions
CALL `_add_fk_if_missing`('ls_name_backgrounds_owned', 'fk_lsnbo_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('ls_name_backgrounds_owned', 'fk_lsnbo_bg', 'name_background_id', 'ls_name_backgrounds', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('ls_name_colors_owned', 'fk_lsnco_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('ls_name_colors_owned', 'fk_lsnco_color', 'name_color_id', 'ls_name_colors', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('ls_prefixes_owned', 'fk_lspo_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('ls_prefixes_owned', 'fk_lspo_prefix', 'prefix_id', 'ls_prefixes', 'id', 'CASCADE');

-- 5.59 Navigator_publics → Navigator_publiccats, Rooms
CALL `_add_fk_if_missing`('navigator_publics', 'fk_navpub_cat', 'public_cat_id', 'navigator_publiccats', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('navigator_publics', 'fk_navpub_room', 'room_id', 'rooms', 'id', 'CASCADE');

-- 5.60 GOTW winners → Users
CALL `_add_fk_if_missing`('gotw_winners', 'fk_gotw_user', 'user_id', 'users', 'id', 'CASCADE');

-- Clean up helper procedure
DROP PROCEDURE IF EXISTS `_add_fk_if_missing`;


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
