-- Runtime paths used by the Windows launcher installation.
INSERT INTO `emulator_settings` (`key`, `value`, `comment`) VALUES
    ('imager.internal.enabled', '1', 'Enable the internal guild badge renderer.'),
    ('imager.location.badgeparts', 'C:/Users/xAstroBoy/Desktop/Custom Habbo Retro/AtomCMS/public/nitro-assets/c_images/Badgeparts', 'Filesystem path where badge part assets are stored.'),
    ('imager.location.output.badges', 'C:/Users/xAstroBoy/Desktop/Custom Habbo Retro/AtomCMS/public/nitro-assets/c_images/Badgeparts/generated', 'Filesystem output path for generated badges.'),
    ('youtube.apikey', '', 'Google YouTube Data API v3 key used by YouTube TVs.')
ON DUPLICATE KEY UPDATE
    `value` = VALUES(`value`),
    `comment` = VALUES(`comment`);
