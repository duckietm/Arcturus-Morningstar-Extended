ALTER TABLE `rooms` ADD COLUMN `allow_underpass` ENUM('0','1') NOT NULL DEFAULT '0' AFTER `move_diagonally`;
