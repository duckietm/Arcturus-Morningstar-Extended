ALTER TABLE `rooms`
    ADD COLUMN `builders_club_trial_locked` TINYINT(1) NOT NULL DEFAULT 0 AFTER `allow_underpass`,
    ADD COLUMN `builders_club_original_state` VARCHAR(16) NOT NULL DEFAULT 'open' AFTER `builders_club_trial_locked`;

CREATE TABLE IF NOT EXISTS `builders_club_items` (
    `item_id` INT(11) NOT NULL,
    `user_id` INT(11) NOT NULL,
    `room_id` INT(11) NOT NULL DEFAULT 0,
    PRIMARY KEY (`item_id`),
    KEY `idx_builders_club_items_user_id` (`user_id`),
    KEY `idx_builders_club_items_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
