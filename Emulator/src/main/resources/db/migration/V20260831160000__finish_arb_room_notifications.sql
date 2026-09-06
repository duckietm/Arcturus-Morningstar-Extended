-- ARB is rank 3 in this hotel. These commands are intentionally unavailable
-- to regular users and VIPs, while remaining enabled for every higher rank.
UPDATE `permission_definitions`
SET `rank_1` = 0, `rank_2` = 0,
    `rank_3` = 1, `rank_4` = 1, `rank_5` = 1, `rank_6` = 1, `rank_7` = 1
WHERE `permission_key` IN (
    'cmd_bss_notification',
    'cmd_bss_open_poker',
    'cmd_bss_open_room',
    'cmd_bss_close_room'
);

-- Keep the English alias for compatibility, but expose the requested Italian command.
INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_bss_open_poker', 'apripoker;openpoker'),
    ('commands.description.cmd_bss_open_poker', ':apripoker - annuncia e apre la zona poker'),
    ('commands.description.cmd_bss_open_room', ':apristanza - rimuove il campanello dalla stanza'),
    ('commands.description.cmd_bss_close_room', ':chiudistanza - mette il campanello alla stanza')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- Every headline/body remains editable from Housekeeping under emulator settings.
INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('bss.notification.arb.headline', 'ARBITRI RICHIESTI'),
    ('bss.notification.arb.text', 'Vieni qui per vincere un raro v10 o altri premi!'),
    ('bss.notification.staff.headline', 'STAFF RICHIESTO'),
    ('bss.notification.staff.text', 'Vieni qui per vincere un raro v10 o altri premi!'),
    ('bss.notification.pok.headline', 'ZONA POKER APERTA'),
    ('bss.notification.pok.text', 'Ora e aperto un poker da %user%. Clicca qui per andare.'),
    ('bss.notification.evento.headline', 'EVENTO IN CORSO'),
    ('bss.notification.evento.text', 'Vieni qui per vincere un raro v10 o altri premi!')
ON DUPLICATE KEY UPDATE `value` = `value`;
