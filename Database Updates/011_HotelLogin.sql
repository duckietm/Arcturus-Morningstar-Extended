ALTER TABLE emulator_settings
CHANGE COLUMN `comment` `comment` TEXT NULL DEFAULT '' ;

CREATE TABLE IF NOT EXISTS `password_resets` (
    `user_id`     INT          NOT NULL PRIMARY KEY,
    `token`       VARCHAR(128) NOT NULL,
    `expires_at`  TIMESTAMP    NOT NULL,
    `created_ip`  VARCHAR(64)  NOT NULL DEFAULT '',
    UNIQUE KEY `idx_token` (`token`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('login.turnstile.enabled',     '0'),
    ('login.turnstile.sitekey',     ''),
    ('login.turnstile.secretkey',   ''),

    ('login.ratelimit.enabled',     '1'),
    ('login.ratelimit.max_attempts','5'),
    ('login.ratelimit.window_sec',  '60'),
    ('login.ratelimit.lockout_sec', '120'),

    ('login.register.enabled',      '1'),
    ('register.max_per_ip',         '5'),
    ('register.default.look',       'hr-100-7.hd-180-1.ch-210-66.lg-270-82.sh-290-80'),
    ('register.default.motto',      'I love Habbo!'),

    ('password.reset.url',          'http://localhost/reset-password'),

    ('smtp.provider',               'own'),
    ('smtp.host',                   'localhost'),
    ('smtp.port',                   '587'),
    ('smtp.username',               ''),
    ('smtp.password',               ''),
    ('smtp.from_address',           'no-reply@example.com'),
    ('smtp.from_name',              'Habbo Hotel'),
    ('smtp.use_tls',                '1'),
    ('smtp.use_ssl',                '0')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
