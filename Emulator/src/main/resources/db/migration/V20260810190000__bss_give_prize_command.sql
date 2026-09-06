INSERT INTO `permission_definitions`
    (`permission_key`, `max_value`, `comment`, `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('cmd_bss_give_prize', 1, 'Administrator-only random BSS rare prize command.', 0, 0, 0, 0, 0, 0, 1)
ON DUPLICATE KEY UPDATE
    `max_value` = 1,
    `comment` = VALUES(`comment`),
    `rank_1` = 0,
    `rank_2` = 0,
    `rank_3` = 0,
    `rank_4` = 0,
    `rank_5` = 0,
    `rank_6` = 0,
    `rank_7` = 1;

INSERT INTO `emulator_settings` (`key`, `value`)
VALUES ('bss.commands.give_prize.max_quantity', '25')
ON DUPLICATE KEY UPDATE `value` = `value`;

INSERT INTO `emulator_texts` (`key`, `value`)
VALUES
    ('commands.keys.cmd_bss_give_prize', 'giveprize'),
    ('commands.description.cmd_bss_give_prize', ':giveprize <utente> <valore> [quantita] - consegna rari BSS casuali dello stesso valore.'),
    ('commands.error.cmd_bss_give_prize.usage', 'Utilizzo: :giveprize <utente> <valore> [quantita]'),
    ('commands.error.cmd_bss_give_prize.invalid_value', 'Il valore deve essere un numero positivo.'),
    ('commands.error.cmd_bss_give_prize.invalid_quantity', 'La quantita deve essere compresa tra 1 e %max%.'),
    ('commands.error.cmd_bss_give_prize.user_not_found', 'Utente %user% non trovato.'),
    ('commands.error.cmd_bss_give_prize.user_offline', '%user% deve essere online per ricevere il premio.'),
    ('commands.error.cmd_bss_give_prize.no_rares', 'Nessun raro BSS disponibile con valore %value%.'),
    ('commands.error.cmd_bss_give_prize.storage', 'Premio non consegnato: errore durante il salvataggio.'),
    ('commands.success.cmd_bss_give_prize', 'Consegnati %quantity% rari casuali a %user% (valore %value%): %items%'),
    ('commands.generic.cmd_bss_give_prize.received', 'Hai ricevuto un premio dallo staff!')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
