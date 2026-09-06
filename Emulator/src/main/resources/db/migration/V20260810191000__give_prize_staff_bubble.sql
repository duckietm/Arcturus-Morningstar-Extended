INSERT INTO `emulator_texts` (`key`, `value`)
VALUES
    ('commands.error.cmd_bss_give_prize.user_not_in_room', '%user% deve trovarsi in una stanza per ricevere il fumetto del premio.'),
    ('commands.generic.cmd_bss_give_prize.received_one', 'Hai ricevuto un premio dallo staff: %items% (valore BSS: %value%). Lo trovi nel tuo inventario!'),
    ('commands.generic.cmd_bss_give_prize.received_many', 'Hai ricevuto %quantity% premi dallo staff, valore BSS %value% ciascuno: %items%. Li trovi nel tuo inventario!')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

DELETE FROM `emulator_texts`
WHERE `key` = 'commands.generic.cmd_bss_give_prize.received';
