-- AIR 13 quest engine: campaign quests, the daily quest pool, the daily
-- tasks ("daily reward" window) and the reward track.
--
-- Wire: GetQuests 3333 / Quests 3625, Quest 230, QuestCompleted 949,
-- QuestCancelled 3027, SeasonalQuests 1122, GetDailyQuest 2486 /
-- QuestDaily 1878, AcceptQuest 3604, ActivateQuest 793, RejectQuest 2397,
-- CancelDailyQuest 3133, OpenQuestTracker 2750, StartCampaign 1697;
-- GetDailyTasks 4100 / ActiveDailyTasks 2900, DailyTasksAdded 670,
-- ClaimDailyTask 4101 / DailyTaskUpdated 9450; RewardTracks 2327,
-- ClaimRewardTrackPrize 1111 / RewardTrackClaimResult 9451,
-- RewardTrackProgress 9452, PurchaseRewardTrackPremium 3022 /
-- RewardTrackPremiumPurchaseResult 2248.
--
-- Definitions are data: `quests`, `daily_tasks`, `reward_tracks` and their
-- children are edited by the hotel, the `users_*` tables hold progress.
-- Goal types (quests.goal_type, daily_tasks.goal_type): TALK_IN_ROOM,
-- VISIT_ROOMS, PLACE_FURNI, GIVE_RESPECT, COMPLETE_QUEST, CLAIM_DAILY_TASK.
-- reward_track_tasks.action_type uses the official client action names
-- (they pick the task icon): chat_with_someone, enter_other_users_room,
-- place_item, give_respect, complete_quest, claim_daily_task.

CREATE TABLE IF NOT EXISTS `quests` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `campaign_code` VARCHAR(64) NOT NULL,
    `code` VARCHAR(64) NOT NULL,
    `chain_code` VARCHAR(64) NOT NULL DEFAULT '',
    `sort_order` INT NOT NULL DEFAULT 0,
    `goal_type` VARCHAR(32) NOT NULL,
    `goal_data` VARCHAR(255) NOT NULL DEFAULT '',
    `goal_count` INT NOT NULL DEFAULT 1,
    `reward_type` INT NOT NULL DEFAULT 0,
    `reward_amount` INT NOT NULL DEFAULT 0,
    `reward_badge` VARCHAR(32) NOT NULL DEFAULT '',
    `easy` TINYINT(1) NOT NULL DEFAULT 1,
    `daily` TINYINT(1) NOT NULL DEFAULT 0,
    `seasonal_seconds` INT NOT NULL DEFAULT 0,
    `wait_seconds` INT NOT NULL DEFAULT 0,
    `catalog_page_name` VARCHAR(64) NOT NULL DEFAULT '',
    `image_version` VARCHAR(16) NOT NULL DEFAULT '1',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_quests_campaign_code` (`campaign_code`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `users_quests` (
    `user_id` INT NOT NULL,
    `quest_id` INT NOT NULL,
    `progress` INT NOT NULL DEFAULT 0,
    `accepted` TINYINT(1) NOT NULL DEFAULT 0,
    `accepted_at` INT NOT NULL DEFAULT 0,
    `completed_at` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`, `quest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `daily_tasks` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(64) NOT NULL,
    `goal_type` VARCHAR(32) NOT NULL,
    `goal_data` VARCHAR(255) NOT NULL DEFAULT '',
    `required_repeats` INT NOT NULL DEFAULT 1,
    `is_bonus` TINYINT(1) NOT NULL DEFAULT 0,
    `reward_type` VARCHAR(32) NOT NULL DEFAULT 'duckets',
    `reward_extra` VARCHAR(255) NOT NULL DEFAULT '',
    `reward_amount` INT NOT NULL DEFAULT 0,
    `catalog_name` VARCHAR(64) NOT NULL DEFAULT '',
    `image_version` VARCHAR(16) NOT NULL DEFAULT '1',
    `sort_order` INT NOT NULL DEFAULT 0,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_daily_tasks_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `users_daily_tasks` (
    `user_id` INT NOT NULL,
    `task_id` INT NOT NULL,
    `task_day` DATE NOT NULL,
    `repeats` INT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 0,
    `updated_at` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`, `task_id`, `task_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `reward_tracks` (
    `id` VARCHAR(64) NOT NULL,
    `theme` VARCHAR(64) NOT NULL DEFAULT 'blue',
    `sort_order` INT NOT NULL DEFAULT 0,
    `starts_at` INT NOT NULL DEFAULT 0,
    `ends_at` INT NOT NULL DEFAULT 0,
    `has_premium` TINYINT(1) NOT NULL DEFAULT 0,
    `premium_task_points_boost` DOUBLE NOT NULL DEFAULT 0,
    `premium_instant_points` INT NOT NULL DEFAULT 0,
    `premium_cost_diamonds` INT NOT NULL DEFAULT 0,
    `premium_cost_credits` INT NOT NULL DEFAULT 0,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `reward_track_tasks` (
    `track_id` VARCHAR(64) NOT NULL,
    `id` VARCHAR(64) NOT NULL,
    `action_type` VARCHAR(64) NOT NULL,
    `parameter` VARCHAR(255) NOT NULL DEFAULT '',
    `premium` TINYINT(1) NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`track_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `reward_track_task_levels` (
    `track_id` VARCHAR(64) NOT NULL,
    `task_id` VARCHAR(64) NOT NULL,
    `level` INT NOT NULL,
    `required_count` INT NOT NULL DEFAULT 1,
    `points_reward` INT NOT NULL DEFAULT 0,
    `premium` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`track_id`, `task_id`, `level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `reward_track_prizes` (
    `track_id` VARCHAR(64) NOT NULL,
    `id` VARCHAR(64) NOT NULL,
    `required_points` INT NOT NULL DEFAULT 0,
    `product_item_type_id` INT NOT NULL DEFAULT 0,
    `reward_type` VARCHAR(32) NOT NULL DEFAULT 'duckets',
    `extra_params` VARCHAR(255) NOT NULL DEFAULT '',
    `reward_amount` INT NOT NULL DEFAULT 0,
    `premium` TINYINT(1) NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`track_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `users_reward_tracks` (
    `user_id` INT NOT NULL,
    `track_id` VARCHAR(64) NOT NULL,
    `points` INT NOT NULL DEFAULT 0,
    `premium` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`, `track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `users_reward_track_tasks` (
    `user_id` INT NOT NULL,
    `track_id` VARCHAR(64) NOT NULL,
    `task_id` VARCHAR(64) NOT NULL,
    `progress_count` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`, `track_id`, `task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `users_reward_track_prizes` (
    `user_id` INT NOT NULL,
    `track_id` VARCHAR(64) NOT NULL,
    `prize_id` VARCHAR(64) NOT NULL,
    `claimed_at` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`, `track_id`, `prize_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Seed: official-style campaigns. Texts: quests.<campaign>.name and
-- quests.<campaign>.<code>.name / .desc. reward_type is the activity point
-- type (0 duckets, 5 diamonds), -1 means credits.
INSERT IGNORE INTO `quests`
    (`campaign_code`, `code`, `sort_order`, `goal_type`, `goal_count`, `reward_type`, `reward_amount`, `reward_badge`, `easy`, `daily`)
VALUES
    ('chat', 'chat_1', 1, 'TALK_IN_ROOM', 5, 0, 20, '', 1, 0),
    ('chat', 'chat_2', 2, 'TALK_IN_ROOM', 25, 0, 50, '', 0, 0),
    ('chat', 'chat_3', 3, 'TALK_IN_ROOM', 100, -1, 5, 'ACH_RoomEntry1', 0, 0),
    ('explore', 'explore_1', 1, 'VISIT_ROOMS', 3, 0, 20, '', 1, 0),
    ('explore', 'explore_2', 2, 'VISIT_ROOMS', 10, 0, 50, '', 0, 0),
    ('explore', 'explore_3', 3, 'VISIT_ROOMS', 30, -1, 5, '', 0, 0),
    ('decorate', 'decorate_1', 1, 'PLACE_FURNI', 3, 0, 20, '', 1, 0),
    ('decorate', 'decorate_2', 2, 'PLACE_FURNI', 10, 0, 50, '', 0, 0),
    ('decorate', 'decorate_3', 3, 'PLACE_FURNI', 25, -1, 5, '', 0, 0),
    ('social', 'social_1', 1, 'GIVE_RESPECT', 1, 0, 20, '', 1, 0),
    ('social', 'social_2', 2, 'GIVE_RESPECT', 5, 0, 50, '', 0, 0),
    ('social', 'social_3', 3, 'GIVE_RESPECT', 15, -1, 5, '', 0, 0),
    ('daily', 'daily_chat', 1, 'TALK_IN_ROOM', 10, 0, 10, '', 1, 1),
    ('daily', 'daily_visit', 2, 'VISIT_ROOMS', 3, 0, 10, '', 1, 1),
    ('daily', 'daily_furni', 3, 'PLACE_FURNI', 5, 0, 25, '', 0, 1),
    ('daily', 'daily_respect', 4, 'GIVE_RESPECT', 3, 0, 25, '', 0, 1);

INSERT IGNORE INTO `daily_tasks`
    (`code`, `goal_type`, `required_repeats`, `is_bonus`, `reward_type`, `reward_extra`, `reward_amount`, `sort_order`)
VALUES
    ('talk', 'TALK_IN_ROOM', 10, 0, 'duckets', '', 15, 1),
    ('visit', 'VISIT_ROOMS', 3, 0, 'duckets', '', 15, 2),
    ('furni', 'PLACE_FURNI', 3, 0, 'duckets', '', 20, 3),
    ('respect', 'GIVE_RESPECT', 1, 0, 'duckets', '', 20, 4),
    ('bonus', 'CLAIM_DAILY_TASK', 4, 1, 'credits', '', 5, 5);

INSERT IGNORE INTO `reward_tracks`
    (`id`, `theme`, `sort_order`, `starts_at`, `ends_at`, `has_premium`, `premium_task_points_boost`, `premium_instant_points`, `premium_cost_diamonds`, `premium_cost_credits`)
VALUES
    ('season_1', 'blue', 1, 0, 0, 1, 1.5, 50, 25, 0);

INSERT IGNORE INTO `reward_track_tasks` (`track_id`, `id`, `action_type`, `parameter`, `premium`, `sort_order`)
VALUES
    ('season_1', 'talk', 'chat_with_someone', '', 0, 1),
    ('season_1', 'visit', 'enter_other_users_room', '', 0, 2),
    ('season_1', 'furni', 'place_item', '', 0, 3),
    ('season_1', 'respect', 'give_respect', '', 0, 4),
    ('season_1', 'quests', 'complete_quest', '', 0, 5),
    ('season_1', 'daily', 'claim_daily_task', '', 1, 6);

INSERT IGNORE INTO `reward_track_task_levels` (`track_id`, `task_id`, `level`, `required_count`, `points_reward`, `premium`)
VALUES
    ('season_1', 'talk', 1, 25, 10, 0),
    ('season_1', 'talk', 2, 100, 20, 0),
    ('season_1', 'talk', 3, 500, 40, 0),
    ('season_1', 'visit', 1, 10, 10, 0),
    ('season_1', 'visit', 2, 50, 20, 0),
    ('season_1', 'visit', 3, 200, 40, 0),
    ('season_1', 'furni', 1, 10, 10, 0),
    ('season_1', 'furni', 2, 50, 20, 0),
    ('season_1', 'furni', 3, 200, 40, 0),
    ('season_1', 'respect', 1, 5, 10, 0),
    ('season_1', 'respect', 2, 25, 20, 0),
    ('season_1', 'respect', 3, 100, 40, 0),
    ('season_1', 'quests', 1, 3, 20, 0),
    ('season_1', 'quests', 2, 8, 40, 0),
    ('season_1', 'quests', 3, 12, 60, 0),
    ('season_1', 'daily', 1, 5, 20, 1),
    ('season_1', 'daily', 2, 15, 40, 1),
    ('season_1', 'daily', 3, 30, 80, 1);

INSERT IGNORE INTO `reward_track_prizes`
    (`track_id`, `id`, `required_points`, `product_item_type_id`, `reward_type`, `extra_params`, `reward_amount`, `premium`, `sort_order`)
VALUES
    ('season_1', 'p1', 20, 0, 'duckets', '', 50, 0, 1),
    ('season_1', 'p1_premium', 20, 0, 'diamonds', '', 5, 1, 2),
    ('season_1', 'p2', 60, 0, 'credits', '', 10, 0, 3),
    ('season_1', 'p2_premium', 60, 0, 'diamonds', '', 10, 1, 4),
    ('season_1', 'p3', 120, 0, 'duckets', '', 150, 0, 5),
    ('season_1', 'p3_premium', 120, 0, 'credits', '', 25, 1, 6),
    ('season_1', 'p4', 200, 0, 'badge', 'ACH_RoomEntry2', 1, 0, 7),
    ('season_1', 'p4_premium', 200, 0, 'diamonds', '', 25, 1, 8);
