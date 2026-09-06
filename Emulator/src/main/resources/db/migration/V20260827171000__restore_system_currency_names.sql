-- Human-readable names used by :givepoints and housekeeping responses.
INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('seasonal.name.0', 'Duckets'),
    ('seasonal.name.1', 'Pixels'),
    ('seasonal.name.2', 'Seasonal Points'),
    ('seasonal.name.3', 'Event Points'),
    ('seasonal.name.4', 'Shells'),
    ('seasonal.name.5', 'Diamonds'),
    ('seasonal.name.101', 'Snowflakes'),
    ('seasonal.name.102', 'Hearts'),
    ('seasonal.name.103', 'Punti'),
    ('seasonal.name.104', 'Clouds'),
    ('seasonal.name.105', 'Gems')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
