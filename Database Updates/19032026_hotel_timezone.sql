SET NAMES utf8mb4;

-- Create the hotel timezone setting if it does not exist yet.
INSERT INTO `emulator_settings` (`key`, `value`)
SELECT 'hotel.timezone', 'Europe/Rome'
WHERE NOT EXISTS (
    SELECT 1
    FROM `emulator_settings`
    WHERE `key` = 'hotel.timezone'
);

-- Keep the default/example value aligned for existing installs too.
UPDATE `emulator_settings`
SET `value` = 'Europe/Rome'
WHERE `key` = 'hotel.timezone';

-- Helper query for a timezone selector.
-- If MySQL/MariaDB timezone tables are populated, this returns the available timezone ids.
SELECT `Name` AS `timezone_id`
FROM `mysql`.`time_zone_name`
WHERE `Name` IS NOT NULL
ORDER BY `Name`;
