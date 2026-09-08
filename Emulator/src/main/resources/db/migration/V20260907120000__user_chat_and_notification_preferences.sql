-- Per-user preferences the official AIR 13 client persists server-side and reads back from the
-- UserSettings packet: chat mode (0 free flow / 1 line by line), chat bubble width
-- (0 wide / 1 normal / 2 thin), chat scroll speed (0 fast / 1 normal / 2 slow), the friend-online
-- notification preference (0 everyone / 1 relationships / 2 nobody) and the wired whisper switch.

ALTER TABLE `users_settings`
    ADD COLUMN IF NOT EXISTS `chat_mode` INT NOT NULL DEFAULT 0 AFTER `volume_soundboard`,
    ADD COLUMN IF NOT EXISTS `chat_bubble_width` INT NOT NULL DEFAULT 1 AFTER `chat_mode`,
    ADD COLUMN IF NOT EXISTS `chat_scroll_speed` INT NOT NULL DEFAULT 1 AFTER `chat_bubble_width`,
    ADD COLUMN IF NOT EXISTS `online_indicator_preference` INT NOT NULL DEFAULT 0 AFTER `chat_scroll_speed`,
    ADD COLUMN IF NOT EXISTS `wired_whisper_disabled` ENUM('0','1') NOT NULL DEFAULT '0' AFTER `online_indicator_preference`;
