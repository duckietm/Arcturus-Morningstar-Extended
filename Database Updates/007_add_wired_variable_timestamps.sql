ALTER TABLE `room_user_wired_variables`
    ADD COLUMN IF NOT EXISTS `created_at` int(11) NOT NULL DEFAULT 0 AFTER `value`;

ALTER TABLE `room_user_wired_variables`
    ADD COLUMN IF NOT EXISTS `updated_at` int(11) NOT NULL DEFAULT 0 AFTER `created_at`;

UPDATE `room_user_wired_variables`
SET
    `created_at` = IF(`created_at` > 0, `created_at`, UNIX_TIMESTAMP()),
    `updated_at` = IF(`updated_at` > 0, `updated_at`, IF(`created_at` > 0, `created_at`, UNIX_TIMESTAMP()));

ALTER TABLE `room_furni_wired_variables`
    ADD COLUMN IF NOT EXISTS `created_at` int(11) NOT NULL DEFAULT 0 AFTER `value`;

ALTER TABLE `room_furni_wired_variables`
    ADD COLUMN IF NOT EXISTS `updated_at` int(11) NOT NULL DEFAULT 0 AFTER `created_at`;

UPDATE `room_furni_wired_variables`
SET
    `created_at` = IF(`created_at` > 0, `created_at`, UNIX_TIMESTAMP()),
    `updated_at` = IF(`updated_at` > 0, `updated_at`, IF(`created_at` > 0, `created_at`, UNIX_TIMESTAMP()));

ALTER TABLE `room_wired_variables`
    ADD COLUMN IF NOT EXISTS `created_at` int(11) NOT NULL DEFAULT 0 AFTER `value`;

ALTER TABLE `room_wired_variables`
    ADD COLUMN IF NOT EXISTS `updated_at` int(11) NOT NULL DEFAULT 0 AFTER `created_at`;

UPDATE `room_wired_variables`
SET
    `created_at` = 0,
    `updated_at` = IF(`updated_at` > 0, `updated_at`, UNIX_TIMESTAMP());
