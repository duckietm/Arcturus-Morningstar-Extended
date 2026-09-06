INSERT INTO `emulator_texts` (`key`, `value`)
VALUES
    ('commands.success.cmd_bss_report', 'Segnalazione contro %user% inviata allo staff.'),
    ('commands.error.cmd_bss_report.pending', 'Hai gia una segnalazione in attesa: chiudila o attendi che lo staff la gestisca.')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
