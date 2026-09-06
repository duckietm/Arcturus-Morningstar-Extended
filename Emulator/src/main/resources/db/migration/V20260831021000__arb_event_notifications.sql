INSERT INTO `permission_definitions`
    (`permission_key`, `max_value`, `comment`, `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('cmd_bss_notification', 1, 'Invia notifiche cliccabili ARB, staff, POK ed evento', 1, 1, 1, 1, 1, 1, 1),
    ('cmd_bss_open_poker', 1, 'Apre il poker con una notifica cliccabile', 1, 1, 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
    `comment` = VALUES(`comment`),
    `rank_1` = 1, `rank_2` = 1, `rank_3` = 1, `rank_4` = 1,
    `rank_5` = 1, `rank_6` = 1, `rank_7` = 1;

INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('bss.notification.arb.text', 'Gli arbitri sono richiesti'),
    ('bss.notification.staff.text', 'Lo staff e richiesto'),
    ('bss.notification.pok.text', 'Il poker e aperto'),
    ('bss.notification.evento.text', 'Un evento e iniziato')
ON DUPLICATE KEY UPDATE `value` = `value`;

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_bss_notification', 'notifica'),
    ('commands.keys.cmd_bss_open_poker', 'openpoker'),
    ('commands.description.cmd_bss_notification', ':notifica <arb|staff|pok|evento>'),
    ('commands.description.cmd_bss_open_poker', ':openpoker'),
    ('commands.error.cmd_bss_notification.usage', 'Utilizzo: :notifica <arb|staff|pok|evento>. Devi essere in una stanza.'),
    ('commands.success.cmd_bss_notification', 'Notifica %type% inviata per la stanza %room%.'),
    ('commands.keys.cmd_blockalert', 'blockalerts;blockalert;ignorealerts;ignore_alerts;disablealert'),
    ('commands.description.cmd_blockalert', ':disablealert - attiva o disattiva gli avvisi hotel'),
    ('commands.description.cmd_bss_open_room', ':apristanza'),
    ('commands.description.cmd_bss_close_room', ':chiudistanza')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
