-- Normalizes the legacy `permissions` table into:
-- 1. `permission_ranks`       -> one row per rank with rank metadata.
-- 2. `permission_definitions` -> one row per permission key with comments and one `rank_<id>` column per rank.
--
-- This migration keeps the old `permissions` table untouched so the emulator can safely fall back to it.
-- It also cleans up the older experimental normalized objects if they were already created.

DROP VIEW IF EXISTS `permissions_matrix_view`;
DROP PROCEDURE IF EXISTS `refresh_permissions_matrix_view`;
DROP TABLE IF EXISTS `permission_rank_values`;
DROP TABLE IF EXISTS `permission_nodes`;

CREATE TABLE IF NOT EXISTS `permission_ranks` (
  `id` int(11) NOT NULL,
  `rank_name` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `hidden_rank` tinyint(1) NOT NULL DEFAULT 0,
  `badge` varchar(12) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '',
  `job_description` varchar(255) NOT NULL DEFAULT 'Here to help',
  `staff_color` varchar(8) NOT NULL DEFAULT '#327fa8',
  `staff_background` varchar(255) NOT NULL DEFAULT 'staff-bg.png',
  `level` int(11) NOT NULL DEFAULT 1,
  `room_effect` int(11) NOT NULL DEFAULT 0,
  `log_commands` enum('0','1') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0',
  `prefix` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '',
  `prefix_color` varchar(7) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '',
  `auto_credits_amount` int(11) DEFAULT 0,
  `auto_pixels_amount` int(11) DEFAULT 0,
  `auto_gotw_amount` int(11) DEFAULT 0,
  `auto_points_amount` int(11) DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `permission_definitions` (
  `permission_key` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `max_value` tinyint(3) unsigned NOT NULL DEFAULT 1,
  `comment` text NOT NULL,
  PRIMARY KEY (`permission_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci ROW_FORMAT=DYNAMIC;

ALTER TABLE `permission_definitions`
  DROP COLUMN IF EXISTS `category`,
  DROP COLUMN IF EXISTS `value_type`,
  DROP COLUMN IF EXISTS `sort_order`;

INSERT INTO `permission_ranks` (
  `id`,
  `rank_name`,
  `hidden_rank`,
  `badge`,
  `job_description`,
  `staff_color`,
  `staff_background`,
  `level`,
  `room_effect`,
  `log_commands`,
  `prefix`,
  `prefix_color`,
  `auto_credits_amount`,
  `auto_pixels_amount`,
  `auto_gotw_amount`,
  `auto_points_amount`
)
SELECT
  `id`,
  `rank_name`,
  `hidden_rank`,
  `badge`,
  `job_description`,
  `staff_color`,
  `staff_background`,
  `level`,
  `room_effect`,
  `log_commands`,
  `prefix`,
  `prefix_color`,
  `auto_credits_amount`,
  `auto_pixels_amount`,
  `auto_gotw_amount`,
  `auto_points_amount`
FROM `permissions`
ON DUPLICATE KEY UPDATE
  `rank_name` = VALUES(`rank_name`),
  `hidden_rank` = VALUES(`hidden_rank`),
  `badge` = VALUES(`badge`),
  `job_description` = VALUES(`job_description`),
  `staff_color` = VALUES(`staff_color`),
  `staff_background` = VALUES(`staff_background`),
  `level` = VALUES(`level`),
  `room_effect` = VALUES(`room_effect`),
  `log_commands` = VALUES(`log_commands`),
  `prefix` = VALUES(`prefix`),
  `prefix_color` = VALUES(`prefix_color`),
  `auto_credits_amount` = VALUES(`auto_credits_amount`),
  `auto_pixels_amount` = VALUES(`auto_pixels_amount`),
  `auto_gotw_amount` = VALUES(`auto_gotw_amount`),
  `auto_points_amount` = VALUES(`auto_points_amount`);

DROP PROCEDURE IF EXISTS `refresh_permission_definition_rank_columns`;

DELIMITER $$
CREATE PROCEDURE `refresh_permission_definition_rank_columns`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE current_rank_id INT;
  DECLARE current_column_name VARCHAR(32);
  DECLARE column_exists INT DEFAULT 0;
  DECLARE rank_cursor CURSOR FOR SELECT `id` FROM `permission_ranks` ORDER BY `id` ASC;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN rank_cursor;

  rank_loop: LOOP
    FETCH rank_cursor INTO current_rank_id;

    IF done = 1 THEN
      LEAVE rank_loop;
    END IF;

    SET current_column_name = CONCAT('rank_', current_rank_id);

    SELECT COUNT(*)
      INTO column_exists
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'permission_definitions'
      AND `column_name` = current_column_name;

    IF column_exists = 0 THEN
      SET @alter_permissions_column_sql = CONCAT(
        'ALTER TABLE `permission_definitions` ADD COLUMN `',
        current_column_name,
        '` tinyint(3) unsigned NOT NULL DEFAULT 0'
      );

      PREPARE alter_permissions_column_stmt FROM @alter_permissions_column_sql;
      EXECUTE alter_permissions_column_stmt;
      DEALLOCATE PREPARE alter_permissions_column_stmt;
    END IF;
  END LOOP;

  CLOSE rank_cursor;
END$$
DELIMITER ;

CALL `refresh_permission_definition_rank_columns`();

INSERT INTO `permission_definitions` (
  `permission_key`,
  `max_value`,
  `comment`
)
SELECT
  `column_name` AS `permission_key`,
  CASE
    WHEN `column_type` LIKE '%''2''%' THEN 2
    ELSE 1
  END AS `max_value`,
  CASE
    WHEN COALESCE(`column_comment`, '') <> '' THEN `column_comment`
    WHEN `column_name` LIKE 'cmd\_%' AND `column_type` LIKE '%''2''%' THEN CONCAT(
      'Controls access to the :',
      REPLACE(SUBSTRING(`column_name`, 5), '_', ' '),
      ' command. Values: 0 = disabled, 1 = allowed, 2 = allowed only when room-owner rights may be used.'
    )
    WHEN `column_name` LIKE 'cmd\_%' THEN CONCAT(
      'Controls access to the :',
      REPLACE(SUBSTRING(`column_name`, 5), '_', ' '),
      ' command. Values: 0 = disabled, 1 = allowed.'
    )
    WHEN `column_name` LIKE 'acc\_%' AND `column_type` LIKE '%''2''%' THEN CONCAT(
      'Controls the ',
      REPLACE(SUBSTRING(`column_name`, 5), '_', ' '),
      ' capability for this rank. Values: 0 = disabled, 1 = enabled, 2 = enabled only when room-owner rights may be used.'
    )
    WHEN `column_name` LIKE 'acc\_%' THEN CONCAT(
      'Controls the ',
      REPLACE(SUBSTRING(`column_name`, 5), '_', ' '),
      ' capability for this rank. Values: 0 = disabled, 1 = enabled.'
    )
    ELSE CONCAT(
      'Legacy permission-related value migrated from the old permissions table for ',
      `column_name`,
      '.'
    )
  END AS `comment`
FROM `information_schema`.`columns`
WHERE `table_schema` = DATABASE()
  AND `table_name` = 'permissions'
  AND `column_name` NOT IN (
    'id',
    'rank_name',
    'hidden_rank',
    'badge',
    'job_description',
    'staff_color',
    'staff_background',
    'level',
    'room_effect',
    'log_commands',
    'prefix',
    'prefix_color',
    'auto_credits_amount',
    'auto_pixels_amount',
    'auto_gotw_amount',
    'auto_points_amount'
  )
ON DUPLICATE KEY UPDATE
  `max_value` = VALUES(`max_value`),
  `comment` = VALUES(`comment`);

DROP TEMPORARY TABLE IF EXISTS `tmp_permission_comments`;

CREATE TEMPORARY TABLE `tmp_permission_comments` (
  `permission_key` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `comment` text NOT NULL,
  PRIMARY KEY (`permission_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci;

INSERT INTO `tmp_permission_comments` (`permission_key`, `comment`) VALUES
('cmd_about', 'Allows using :about to display emulator, revision, or hotel information exposed by the command.'),
('cmd_alert', 'Allows using :alert to send a hotel alert popup to a specific user.'),
('cmd_allow_trading', 'Allows using the trading-toggle command to enable or disable trading for a target user.'),
('cmd_badge', 'Allows granting a badge code to a target user through a command.'),
('cmd_ban', 'Allows banning users from the hotel.'),
('cmd_blockalert', 'Allows sending the block-alert style moderation message.'),
('cmd_bots', 'Allows using :bots to list the bots currently placed in the room.'),
('cmd_bundle', 'Allows using :bundle / :roombundle to create a catalog room-bundle offer for the current room.'),
('cmd_calendar', 'Allows using the hotel calendar command and any calendar actions wired to that command entry.'),
('cmd_changename', 'Allows forcing a user-name change through the change-name command flow.'),
('cmd_chatcolor', 'Allows changing the active chat bubble color through the chat-color command.'),
('cmd_commands', 'Allows using :commands to list the command keys available to the current user.'),
('cmd_connect_camera', 'Allows using the command that links the in-room camera feature to the current room session.'),
('cmd_control', 'Allows using :control to take over another in-room user and stop controlling them later.'),
('cmd_coords', 'Allows using :coords to inspect room coordinates for tiles, users, or furniture.'),
('cmd_credits', 'Allows giving or removing credits from a user through the staff currency command.'),
('cmd_subscription', 'Allows granting or editing subscription time through the subscription command.'),
('cmd_danceall', 'Allows forcing every Habbo currently in the room to dance.'),
('cmd_diagonal', 'Allows toggling diagonal walking for the current room.'),
('cmd_disconnect', 'Allows disconnecting a user from the hotel immediately.'),
('cmd_duckets', 'Allows giving or removing duckets from a user through the staff currency command.'),
('cmd_ejectall', 'Allows ejecting all users from the current room.'),
('cmd_empty', 'Allows clearing the current user furniture inventory through the empty-inventory command.'),
('cmd_empty_bots', 'Allows clearing the current user bot inventory through the empty-bots command.'),
('cmd_empty_pets', 'Allows clearing the current user pet inventory through the empty-pets command.'),
('cmd_enable', 'Allows applying an avatar effect to yourself, or to another user when acc_enable_others is also granted.'),
('cmd_event', 'Allows marking the current room as an event room through the event command.'),
('cmd_faceless', 'Allows toggling the faceless avatar visual state on the executing room unit.'),
('cmd_fastwalk', 'Allows toggling fast-walk mode for yourself or another in-room user.'),
('cmd_filterword', 'Allows adding or removing entries from the configured word filter through command usage.'),
('cmd_freeze', 'Allows freezing a target user in place.'),
('cmd_freeze_bots', 'Allows freezing bots that are placed in the room.'),
('cmd_gift', 'Allows sending a gift to a target user through the gift command.'),
('cmd_give_rank', 'Allows setting another user rank through the give-rank command.'),
('cmd_ha', 'Allows sending a hotel-wide alert.'),
('acc_can_stalk', 'Allows following users even when they have disabled stalking.'),
('cmd_hal', 'Allows sending a hotel-wide alert with a clickable link or extended content.'),
('cmd_invisible', 'Allows toggling invisible staff mode.'),
('cmd_ip_ban', 'Allows banning a user by IP address.'),
('cmd_machine_ban', 'Allows banning a user by machine identifier.'),
('cmd_hand_item', 'Allows spawning or changing the hand item currently held by a user.'),
('cmd_happyhour', 'Allows starting or stopping the happy-hour event flow exposed by the happyhour command.'),
('cmd_hidewired', 'Allows toggling whether wired furniture is visually hidden in the current room.'),
('cmd_kickall', 'Allows kicking every user from the current room.'),
('cmd_softkick', 'Allows soft-kicking a user back to the hotel view without a full sanction.'),
('cmd_massbadge', 'Allows giving the same badge to many users at once.'),
('cmd_roombadge', 'Allows setting or overriding the room badge shown to users.'),
('cmd_masscredits', 'Allows giving credits to many users at once through the mass-credits command.'),
('cmd_massduckets', 'Allows giving duckets to many users at once through the mass-duckets command.'),
('cmd_massgift', 'Allows sending the same gift to many users at once.'),
('cmd_masspoints', 'Allows giving activity points to many users at once through the mass-points command.'),
('cmd_moonwalk', 'Allows toggling the moonwalk avatar effect for yourself while you are inside a room.'),
('cmd_mimic', 'Allows copying another user appearance or presence state through the mimic command.'),
('cmd_multi', 'Allows executing multiple chat commands from the special sticky/post-it scripting payload.'),
('cmd_mute', 'Allows muting a target user.'),
('cmd_pet_info', 'Allows opening the detailed pet-information view for a pet.'),
('cmd_pickall', 'Allows picking up every furniture item from the current room.'),
('cmd_plugins', 'Legacy key for the :plugins command, which currently lists loaded plugins without enforcing this dedicated permission node in code.'),
('cmd_points', 'Allows giving or removing activity points from a user through the points command.'),
('cmd_promote_offer', 'Allows using :promoteoffer to list active target offers or switch the globally promoted target offer.'),
('cmd_pull', 'Allows pulling a nearby user onto the tile directly in front of you.'),
('cmd_push', 'Allows pushing the user standing in front of you one tile farther in the direction you are facing.'),
('cmd_redeem', 'Allows redeeming redeemable inventory items through the redeem command flow.'),
('cmd_reload_room', 'Allows unloading and reloading the current room, then forwarding the occupants back into the fresh room instance.'),
('cmd_roomalert', 'Allows sending the same alert message to everyone in the current room.'),
('cmd_roomcredits', 'Allows giving credits to every Habbo currently in the room.'),
('cmd_roomeffect', 'Allows applying the same avatar effect id to every Habbo currently in the room.'),
('cmd_roomgift', 'Allows sending the same gift to every Habbo currently in the room.'),
('cmd_roomitem', 'Allows setting the same hand-item id for every Habbo in the room; using 0 clears the hand item.'),
('cmd_roommute', 'Allows muting every Habbo currently in the room.'),
('cmd_roompixels', 'Allows giving duckets or pixels to every Habbo currently in the room.'),
('cmd_roompoints', 'Allows giving activity points to every Habbo currently in the room.'),
('cmd_say', 'Allows forcing another online user to say a custom message in their current room.'),
('cmd_say_all', 'Allows making everyone in the room say a message.'),
('cmd_setmax', 'Allows using :setmax to change the maximum user capacity of the current room.'),
('cmd_set_poll', 'Allows using :setpoll to attach or remove a poll on the current room.'),
('cmd_setpublic', 'Allows using :setpublic to change the room public/private visibility state.'),
('cmd_setspeed', 'Allows using :setspeed to change the room walking speed setting.'),
('cmd_shout', 'Allows forcing another online user to shout a custom message in their current room.'),
('cmd_shout_all', 'Allows making everyone in the room shout a message.'),
('cmd_shutdown', 'Allows using the shutdown command to stop the emulator process.'),
('cmd_sitdown', 'Allows forcing users to sit down through the sitdown command.'),
('cmd_staffalert', 'Allows sending an alert that is visible only to staff members.'),
('cmd_staffonline', 'Allows viewing the current list of online staff members.'),
('cmd_summon', 'Allows summoning a target user into the room where the staff member currently is.'),
('cmd_summonrank', 'Allows summoning all online users of a given rank into the current room.'),
('cmd_super_ban', 'Allows issuing the strongest ban command variant exposed by the super-ban command.'),
('cmd_stalk', 'Allows following another user to their room.'),
('cmd_superpull', 'Allows pulling a user to the tile in front of you without the short-range reach check used by :pull.'),
('cmd_take_badge', 'Allows removing a badge code from a target user.'),
('cmd_talk', 'Allows using the legacy :talk command to make another user speak a command-provided message.'),
('cmd_teleport', 'Allows toggling the room-unit teleport mode used by the :teleport command.'),
('cmd_trash', 'Allows deleting or trashing furniture/items through the trash command flow.'),
('cmd_transform', 'Allows transforming your room unit into a chosen pet type, race, and color.'),
('cmd_unban', 'Allows removing active bans.'),
('cmd_unload', 'Allows disposing the current room instance immediately through :unload / :crash.'),
('cmd_unmute', 'Allows removing an active mute from a target user.'),
('cmd_update_achievements', 'Allows using :update_achievements to reload achievements configuration.'),
('cmd_update_bots', 'Allows using :update_bots to reload bot data and bot configuration.'),
('cmd_update_catalogue', 'Allows using :update_catalogue to reload catalogue pages and offers.'),
('cmd_update_config', 'Allows using :update_config to reload emulator configuration settings.'),
('cmd_update_guildparts', 'Allows using :update_guildparts to reload guild badge parts and guild configuration.'),
('cmd_update_hotel_view', 'Allows using :update_hotel_view to reload hotel-view assets or settings.'),
('cmd_update_items', 'Allows using :update_items to reload item data and furniture definitions.'),
('cmd_update_navigator', 'Allows using :update_navigator to reload navigator configuration and listings.'),
('cmd_update_permissions', 'Allows using :update_permissions to reload ranks and permissions from the database.'),
('cmd_update_pet_data', 'Allows using :update_pet_data to reload pet types and pet races.'),
('cmd_update_plugins', 'Allows using :update_plugins to reload plugin data or plugin metadata.'),
('cmd_update_polls', 'Allows using :update_polls to reload poll and questionnaire data.'),
('cmd_update_texts', 'Allows using :update_texts to reload external texts and localizations.'),
('cmd_update_wordfilter', 'Allows using :update_wordfilter to reload the word-filter list.'),
('cmd_userinfo', 'Allows opening the detailed user-information view used by staff tools.'),
('cmd_word_quiz', 'Allows starting a room word-quiz event with a custom question and optional duration.'),
('cmd_warp', 'Allows instantly warping your room unit to a target tile.'),
('acc_anychatcolor', 'Allows selecting any chat bubble color, including normally restricted colors.'),
('acc_anyroomowner', 'Treats the rank as room owner for owner-only checks such as room settings, wired saving, rights management, floorplan editing, and similar room-owner gates.'),
('acc_empty_others', 'Allows :empty, :empty_bots, and :empty_pets to target another user inventory instead of only your own.'),
('acc_enable_others', 'Allows :enable to apply avatar effects to another user instead of only to yourself.'),
('acc_see_whispers', 'Allows seeing whispers sent between other users in the room.'),
('acc_see_tentchat', 'Allows seeing tent chat or similar hidden chat channels that are normally not visible to everyone.'),
('acc_superwired', 'Allows saving advanced wired data without the normal wordfilter and reward payload restrictions applied to regular users.'),
('acc_supporttool', 'Allows opening and using the support/moderation tool interface.'),
('acc_unkickable', 'Prevents the user from being kicked by normal moderation or room commands.'),
('acc_guildgate', 'Allows bypassing guild gate access restrictions.'),
('acc_moverotate', 'Allows moving, rotating, and saving wired furniture without the usual room-owner restriction checks.'),
('acc_placefurni', 'Allows placing furniture, opening :wired, and passing room-right checks that normally require owner or controller rights.'),
('acc_unlimited_bots', 'Removes both the bot inventory cap and the per-room bot placement cap for this rank.'),
('acc_unlimited_pets', 'Removes both the pet inventory cap and the per-room pet placement cap for this rank.'),
('acc_hide_ip', 'Hides the user IP address in staff tools and other staff-facing views.'),
('acc_hide_mail', 'Hides the user email address in moderation tools and staff views.'),
('acc_not_mimiced', 'Prevents other users from mimicking this account.'),
('acc_chat_no_flood', 'Exempts the user from flood protection limits.'),
('acc_staff_chat', 'Allows accessing staff-only chat channels and staff broadcasts.'),
('acc_staff_pick', 'Allows using staff item pick-up actions that bypass normal room ownership restrictions.'),
('acc_enteranyroom', 'Allows entering rooms regardless of door mode, bans, or normal access restrictions.'),
('acc_fullrooms', 'Allows entering rooms even when they are at maximum user capacity.'),
('acc_infinite_credits', 'Prevents credits from being consumed when a command or purchase checks credit balance.'),
('acc_infinite_pixels', 'Prevents duckets or pixels from being consumed when the balance is checked.'),
('acc_infinite_points', 'Prevents activity points from being consumed when the balance is checked.'),
('acc_ambassador', 'Marks the rank as an ambassador for ambassador-only tools and visuals.'),
('acc_debug', 'Allows using debug-only features, commands, or internal tooling.'),
('acc_chat_no_limit', 'Lets the user hear and be heard regardless of room hearing distance limits.'),
('acc_chat_no_filter', 'Bypasses the word filter for chat and staff-generated messages.'),
('acc_nomute', 'Prevents the user from being muted by normal mute checks.'),
('acc_guild_admin', 'Allows bypassing guild admin restrictions when managing guilds.'),
('acc_catalog_ids', 'Allows seeing internal catalogue page ids, offer ids, or related technical catalogue identifiers.'),
('acc_modtool_ticket_q', 'Allows seeing and handling the moderation ticket queue.'),
('acc_modtool_user_logs', 'Allows reading user chat logs in the moderation tool.'),
('acc_modtool_user_alert', 'Allows sending moderation alerts or cautions to users.'),
('acc_modtool_user_kick', 'Allows kicking users from the moderation tool.'),
('acc_modtool_user_ban', 'Allows banning users from the moderation tool.'),
('acc_modtool_room_info', 'Allows viewing room information in the moderation tool.'),
('acc_modtool_room_logs', 'Allows viewing room chat logs in the moderation tool.'),
('acc_trade_anywhere', 'Allows starting trades outside the normal trade-enabled areas.'),
('acc_update_notifications', 'Allows receiving update notifications emitted by the emulator.'),
('acc_helper_use_guide_tool', 'Allows opening the helper guide tool.'),
('acc_helper_give_guide_tours', 'Allows accepting and handling guide tour requests.'),
('acc_helper_judge_chat_reviews', 'Allows reviewing helper or chat review tickets.'),
('acc_floorplan_editor', 'Allows opening and saving the floorplan editor.'),
('acc_camera', 'Allows using the in-room camera feature and related camera UI actions.'),
('acc_ads_background', 'Allows editing room advertisement backgrounds.'),
('cmd_wordquiz', 'Legacy alias of cmd_word_quiz for starting a room word-quiz event.'),
('acc_room_staff_tags', 'Shows staff tags or markers above the user while inside rooms.'),
('acc_infinite_friends', 'Removes the normal friend-list size limit.'),
('acc_mimic_unredeemed', 'Allows mimicking looks even when they contain unreleased or restricted clothing.'),
('cmd_update_youtube_playlists', 'Allows reloading YouTube playlist configuration for furniture integrations.'),
('cmd_add_youtube_playlist', 'Allows adding a new YouTube playlist entry.'),
('acc_mention', 'Allows using mention-related chat features beyond the normal rank restriction.'),
('cmd_setstate', 'Legacy room-editor permission for :setstate / :ss, used to change the selected furni state or extradata value.'),
('cmd_buildheight', 'Legacy room-editor permission for :buildheight / :bh, used to change the room build-height override.'),
('cmd_setrotation', 'Legacy room-editor permission for :setrotation / :rot, used to change the rotation of the selected furni.'),
('cmd_sellroom', 'Allows putting the current room up for sale through the sell-room command.'),
('cmd_buyroom', 'Allows purchasing a room that is marked as for sale through the buy-room command.'),
('cmd_pay', 'Allows transferring currency to another user through the pay command.'),
('cmd_kill', 'Allows using the kill command effect exposed by the current command set.'),
('cmd_hoverboard', 'Allows toggling the hoverboard effect or hoverboard movement mode.'),
('cmd_kiss', 'Allows using the kiss interaction command on another user.'),
('cmd_hug', 'Allows using the hug interaction command on another user.'),
('cmd_welcome', 'Allows triggering the welcome command behavior defined by the current command set.'),
('cmd_disable_effects', 'Allows disabling active avatar effects through the disable-effects command.'),
('cmd_brb', 'Allows toggling the be-right-back status command.'),
('cmd_nuke', 'Allows using the nuke command exposed by the current command set.'),
('cmd_slime', 'Allows applying the slime command/effect exposed by the current command set.'),
('cmd_explain', 'Allows using the explain command to send the predefined explanation/help flow to users.'),
('cmd_closedice', 'Legacy essentials permission for :closedice, used to close dice items in the room or all dice at once.'),
('acc_closedice_room', 'Legacy companion permission used by older closed-dice room checks.'),
('cmd_set', 'Legacy essentials permission for :set / :changefurni, the generic furni editing command documented by :set info.'),
('cmd_furnidata', 'Allows viewing technical furnidata information in-game for selected furniture.'),
('kiss_cmd', 'Legacy alias used for the kiss command permission.'),
('acc_calendar_force', 'Allows claiming calendar rewards even when the normal day-difference timing check would block the claim.'),
('cmd_update_calendar', 'Allows using :update_calendar to reload calendar definitions and rewards.'),
('cmd_update_all', 'Allows using :update_all to reload all supported runtime data sets in one command.'),
('cms_dance', 'Legacy CMS-side permission kept for website integrations; no direct in-emulator command handler was found in the current tree.'),
('acc_catalogfurni', 'Allows using catalogue administration features related to furniture pages and offers.'),
('acc_unignorable', 'Prevents the account from being ignored by other users through the ignore system.'),
('cmd_update_chat_bubbles', 'Allows using :update_chat_bubbles to reload chat-bubble definitions and assets.'),
('cmd_calendar_staff', 'Allows the staff-only actions exposed by the calendar command flow.');

UPDATE `permission_definitions` pd
INNER JOIN `tmp_permission_comments` tc ON tc.`permission_key` = pd.`permission_key`
SET pd.`comment` = tc.`comment`;

DROP TEMPORARY TABLE IF EXISTS `tmp_permission_comments`;

DROP PROCEDURE IF EXISTS `refresh_permission_definition_values`;

DELIMITER $$
CREATE PROCEDURE `refresh_permission_definition_values`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE current_rank_id INT;
  DECLARE current_column_name VARCHAR(32);
  DECLARE rank_cursor CURSOR FOR SELECT `id` FROM `permission_ranks` ORDER BY `id` ASC;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN rank_cursor;

  rank_loop: LOOP
    FETCH rank_cursor INTO current_rank_id;

    IF done = 1 THEN
      LEAVE rank_loop;
    END IF;

    SET current_column_name = CONCAT('rank_', current_rank_id);

    SELECT GROUP_CONCAT(
      CONCAT(
        'SELECT ''',
        REPLACE(`column_name`, '''', ''''''),
        ''' AS permission_key, CAST(COALESCE(`',
        REPLACE(`column_name`, '`', '``'),
        '`, ''0'') AS UNSIGNED) AS permission_value FROM `permissions` WHERE `id` = ',
        current_rank_id
      )
      ORDER BY `ordinal_position`
      SEPARATOR ' UNION ALL '
    ) INTO @permission_rank_source_sql
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'permissions'
      AND `column_name` NOT IN (
        'id',
        'rank_name',
        'hidden_rank',
        'badge',
        'job_description',
        'staff_color',
        'staff_background',
        'level',
        'room_effect',
        'log_commands',
        'prefix',
        'prefix_color',
        'auto_credits_amount',
        'auto_pixels_amount',
        'auto_gotw_amount',
        'auto_points_amount'
      );

    SET @permission_rank_update_sql = CONCAT(
      'UPDATE `permission_definitions` pd ',
      'INNER JOIN (',
      @permission_rank_source_sql,
      ') src ON src.permission_key = pd.permission_key ',
      'SET pd.`',
      current_column_name,
      '` = src.permission_value'
    );

    PREPARE permission_rank_update_stmt FROM @permission_rank_update_sql;
    EXECUTE permission_rank_update_stmt;
    DEALLOCATE PREPARE permission_rank_update_stmt;
  END LOOP;

  CLOSE rank_cursor;
END$$
DELIMITER ;

CALL `refresh_permission_definition_values`();
