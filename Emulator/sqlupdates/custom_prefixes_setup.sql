-- ============================================================
-- Custom Prefix System - Complete Setup
-- ============================================================

-- 1. Main user prefixes table
CREATE TABLE IF NOT EXISTS `user_prefixes` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `user_id` INT(11) NOT NULL,
    `text` VARCHAR(50) NOT NULL,
    `color` VARCHAR(255) NOT NULL DEFAULT '#FFFFFF',
    `icon` VARCHAR(50) NOT NULL DEFAULT '',
    `effect` VARCHAR(50) NOT NULL DEFAULT '',
    `active` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_active` (`user_id`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Prefix settings table
CREATE TABLE IF NOT EXISTS `custom_prefix_settings` (
    `key_name` VARCHAR(100) NOT NULL,
    `value` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Default settings
INSERT IGNORE INTO `custom_prefix_settings` (`key_name`, `value`) VALUES
    ('max_length', '15'),
    ('min_rank_to_buy', '1'),
    ('price_credits', '5'),
    ('price_points', '0'),
    ('points_type', '0');

-- 3. Blacklisted words table
CREATE TABLE IF NOT EXISTS `custom_prefix_blacklist` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(100) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Example blacklist entries (customize as needed)
INSERT IGNORE INTO `custom_prefix_blacklist` (`word`) VALUES
    ('admin'),
    ('staff'),
    ('mod'),
    ('owner');

-- 4. Add effect column (if table already exists without it)
-- ALTER TABLE `user_prefixes` ADD COLUMN IF NOT EXISTS `effect` VARCHAR(50) NOT NULL DEFAULT '' AFTER `icon`;

-- ============================================================
-- Catalog page for custom prefixes
-- ============================================================
-- NOTE: Adjust parent_id to match your catalog parent category ID.
-- Example: parent_id = -1 for root, or the ID of your "Extra" / "Specials" category

INSERT INTO `catalog_pages` (
    `parent_id`, `caption`, `caption_save`, `icon_image`, `visible`, `enabled`,
    `min_rank`, `page_layout`, `page_strings_1`, `page_strings_2`
) VALUES (
    -1,
    'Custom Prefix',
    'custom_prefix',
    1,
    1,
    1,
    1,
    'custom_prefix',
    'Create your own custom prefix!\rChoose text, colors, icon and effects to stand out in chat.',
    ''
);

-- ============================================================
-- Command texts (insert into emulator_texts if not present)
-- ============================================================
INSERT IGNORE INTO `emulator_texts` (`key`, `value`) VALUES
    -- GivePrefix command
    ('commands.keys.cmd_give_prefix', 'giveprefix'),
    ('commands.error.cmd_give_prefix.usage', 'Usage: :giveprefix <username> <text> <color> [icon] [effect]'),
    ('commands.error.cmd_give_prefix.invalid_color', 'Invalid color format. Use hex format (#FF0000).'),
    ('commands.error.cmd_give_prefix.too_long', 'Prefix text is too long (max 15 characters).'),
    ('commands.error.cmd_give_prefix.user_not_found', 'User not found or not online.'),
    ('commands.succes.cmd_give_prefix', 'Prefix {%prefix%} successfully given to %user%!'),
    -- ListPrefixes command
    ('commands.keys.cmd_list_prefixes', 'listprefixes'),
    ('commands.error.cmd_list_prefixes.usage', 'Usage: :listprefixes <username>'),
    ('commands.error.cmd_list_prefixes.user_not_found', 'User not found or not online.'),
    ('commands.succes.cmd_list_prefixes.header', 'Prefixes of %user%:'),
    ('commands.succes.cmd_list_prefixes.empty', '%user% has no prefixes.'),
    -- RemovePrefix command
    ('commands.keys.cmd_remove_prefix', 'removeprefix'),
    ('commands.error.cmd_remove_prefix.usage', 'Usage: :removeprefix <username> <id|all>'),
    ('commands.error.cmd_remove_prefix.user_not_found', 'User not found or not online.'),
    ('commands.error.cmd_remove_prefix.invalid_id', 'Invalid prefix ID. Must be a number or "all".'),
    ('commands.error.cmd_remove_prefix.not_found', 'Prefix not found for this user.'),
    ('commands.succes.cmd_remove_prefix', 'Prefix #%id% removed from %user%.'),
    ('commands.succes.cmd_remove_prefix.all', 'All prefixes removed from %user%.'),
    -- PrefixBlacklist command
    ('commands.keys.cmd_prefix_blacklist', 'prefixblacklist'),
    ('commands.error.cmd_prefix_blacklist.usage', 'Usage: :prefixblacklist <add|remove|list> [word]'),
    ('commands.error.cmd_prefix_blacklist.empty_word', 'Word cannot be empty.'),
    ('commands.succes.cmd_prefix_blacklist.header', 'Blacklisted prefix words:'),
    ('commands.succes.cmd_prefix_blacklist.empty', 'No blacklisted words.'),
    ('commands.succes.cmd_prefix_blacklist.added', 'Word "%word%" added to prefix blacklist.'),
    ('commands.succes.cmd_prefix_blacklist.removed', 'Word "%word%" removed from prefix blacklist.');

-- ============================================================
-- Permissions for prefix commands (add to permissions table)
-- ============================================================
INSERT IGNORE INTO `permissions` (`id`, `rank_id`, `permission_name`, `setting_type`) VALUES
    (NULL, 7, 'cmd_give_prefix', '1'),
    (NULL, 7, 'cmd_list_prefixes', '1'),
    (NULL, 7, 'cmd_remove_prefix', '1'),
    (NULL, 7, 'cmd_prefix_blacklist', '1');
