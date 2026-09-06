-- Remove the upstream emulator branding from existing and future profiles.
UPDATE `users_settings`
SET `tags` = ''
WHERE TRIM(`tags`) IN ('Arcturus Emulator', 'Arcturus Emulator;');

ALTER TABLE `users_settings`
    MODIFY COLUMN `tags` varchar(255) NOT NULL DEFAULT '';

-- Respect permissions are rank-configurable in housekeeping.
INSERT INTO `permission_definitions`
    (`permission_key`, `max_value`, `comment`, `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('acc_infinite_respect', 1, 'Consente di dare rispetto senza consumare il saldo giornaliero.', 0, 1, 1, 1, 1, 1, 1),
    ('cmd_give_respect_points', 1, 'Consente di assegnare punti rispetto da spendere a un utente.', 0, 0, 0, 0, 0, 0, 1)
ON DUPLICATE KEY UPDATE
    `max_value` = VALUES(`max_value`),
    `comment` = VALUES(`comment`),
    `rank_1` = VALUES(`rank_1`),
    `rank_2` = VALUES(`rank_2`),
    `rank_3` = VALUES(`rank_3`),
    `rank_4` = VALUES(`rank_4`),
    `rank_5` = VALUES(`rank_5`),
    `rank_6` = VALUES(`rank_6`),
    `rank_7` = VALUES(`rank_7`);

INSERT INTO `emulator_settings` (`key`, `value`)
VALUES ('hotel.respect.grant.max', '1000000')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_give_respect_points', 'give_respect_points;giverespectpoints;respectpoints'),
    ('commands.description.cmd_give_respect_points', ':give_respect_points <utente> <quantità> - Aggiunge punti rispetto da spendere.'),
    ('commands.error.cmd_give_respect_points.usage', 'Uso: :give_respect_points <utente> <quantità>'),
    ('commands.error.cmd_give_respect_points.invalid_amount', 'Inserisci una quantità positiva non superiore a %max%.'),
    ('commands.error.cmd_give_respect_points.user_not_found', 'Utente %user% non trovato.'),
    ('commands.error.cmd_give_respect_points.storage', 'Impossibile salvare i punti rispetto. Riprova.'),
    ('commands.success.cmd_give_respect_points', 'Hai assegnato %amount% punti rispetto a %user%. Nuovo saldo: %total%.'),
    ('commands.generic.cmd_give_respect_points.received', 'Hai ricevuto %amount% punti rispetto da spendere. Nuovo saldo: %total%.')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
