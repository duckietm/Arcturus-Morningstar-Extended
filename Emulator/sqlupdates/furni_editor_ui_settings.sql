-- UI Settings: adds ui_settings column to users table
-- Stores per-user UI theme preferences as JSON

ALTER TABLE `users` ADD COLUMN `ui_settings` TEXT DEFAULT NULL AFTER `background_overlay_id`;
