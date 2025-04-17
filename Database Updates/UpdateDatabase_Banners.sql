ALTER TABLE `users` 
ADD COLUMN `background_id` INT(11) NOT NULL DEFAULT 0 AFTER `machine_id`,
ADD COLUMN `background_stand_id` INT(11) NOT NULL DEFAULT 0 AFTER `background_id`,
ADD COLUMN `background_overlay_id` INT(11) NOT NULL DEFAULT 0 AFTER `background_stand_id`;
