INSERT INTO emulator_settings (`key`, `value`) VALUES ('packet.global.rate.limit', '50');

ALTER TABLE `rooms` ADD COLUMN IF NOT EXISTS `youtube_enabled` TINYINT(1) NOT NULL DEFAULT 0;