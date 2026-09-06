-- Keep every configured points currency available to the hotel, but make the
-- public catalogue purchasable with Credits or Diamonds only.

UPDATE `catalog_items`
SET
    `cost_credits` = CASE
        WHEN `cost_credits` > 0 THEN `cost_credits`
        ELSE `cost_points`
    END,
    `cost_points` = 0,
    `points_type` = 5
WHERE `points_type` <> 5;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 0, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 5, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 1, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 2, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 3, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 4, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 101, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 102, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 103, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 104, 0 FROM `users` WHERE `id` > 0;

INSERT IGNORE INTO `users_currency` (`user_id`, `type`, `amount`)
SELECT `id`, 105, 0 FROM `users` WHERE `id` > 0;

UPDATE `emulator_settings`
SET `value` = '0;1;2;3;4;5;101;102;103;104;105'
WHERE `key` = 'seasonal.types';

UPDATE `emulator_settings`
SET `value` = 'ducket;pixel;shell;diamond'
WHERE `key` = 'seasonal.currency.names';
