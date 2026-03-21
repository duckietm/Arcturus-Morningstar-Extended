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