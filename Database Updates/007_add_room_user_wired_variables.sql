CREATE TABLE IF NOT EXISTS `room_user_wired_variables` (
    `room_id` int(11) NOT NULL,
    `user_id` int(11) NOT NULL,
    `variable_item_id` int(11) NOT NULL,
    `value` int(11) DEFAULT NULL,
    `created_at` int(11) NOT NULL DEFAULT 0,
    `updated_at` int(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`room_id`, `user_id`, `variable_item_id`),
    KEY `idx_room_user_wired_variables_room_item` (`room_id`, `variable_item_id`),
    KEY `idx_room_user_wired_variables_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `room_furni_wired_variables` (
    `room_id` int(11) NOT NULL,
    `furni_id` int(11) NOT NULL,
    `variable_item_id` int(11) NOT NULL,
    `value` int(11) DEFAULT NULL,
    `created_at` int(11) NOT NULL DEFAULT 0,
    `updated_at` int(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`room_id`, `furni_id`, `variable_item_id`),
    KEY `idx_room_furni_wired_variables_room_item` (`room_id`, `variable_item_id`),
    KEY `idx_room_furni_wired_variables_furni` (`furni_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `room_wired_variables` (
    `room_id` int(11) NOT NULL,
    `variable_item_id` int(11) NOT NULL,
    `value` int(11) NOT NULL DEFAULT 0,
    `created_at` int(11) NOT NULL DEFAULT 0,
    `updated_at` int(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`room_id`, `variable_item_id`),
    KEY `idx_room_wired_variables_room_item` (`room_id`, `variable_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
