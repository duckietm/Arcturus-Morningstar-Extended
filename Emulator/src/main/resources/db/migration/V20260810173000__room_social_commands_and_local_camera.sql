ALTER TABLE `rooms`
    ADD COLUMN IF NOT EXISTS `pull_enabled` TINYINT(1) NOT NULL DEFAULT 0 AFTER `trade_mode`,
    ADD COLUMN IF NOT EXISTS `push_enabled` TINYINT(1) NOT NULL DEFAULT 0 AFTER `pull_enabled`;

INSERT INTO `permission_definitions`
    (`permission_key`, `max_value`, `comment`, `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('cmd_click_invisible_tiles', 1, 'Room owner can click every invisible WIRED click tile.', 1, 1, 1, 1, 1, 1, 1),
    ('cmd_punch', 1, 'Allows the social punch command.', 1, 1, 1, 1, 1, 1, 1),
    ('cmd_toggle_trade', 1, 'Room owner can toggle trading in the current room.', 1, 1, 1, 1, 1, 1, 1),
    ('cmd_toggle_pull', 1, 'Room owner can toggle pull for the current room.', 1, 1, 1, 1, 1, 1, 1),
    ('cmd_toggle_push', 1, 'Room owner can toggle push for the current room.', 1, 1, 1, 1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
    `max_value`=VALUES(`max_value`),
    `comment`=VALUES(`comment`),
    `rank_1`=VALUES(`rank_1`),
    `rank_2`=VALUES(`rank_2`),
    `rank_3`=VALUES(`rank_3`),
    `rank_4`=VALUES(`rank_4`),
    `rank_5`=VALUES(`rank_5`),
    `rank_6`=VALUES(`rank_6`),
    `rank_7`=VALUES(`rank_7`);

UPDATE `permission_definitions`
SET `rank_1`=1, `rank_2`=1, `rank_3`=1, `rank_4`=1, `rank_5`=1, `rank_6`=1, `rank_7`=1
WHERE `permission_key` IN ('cmd_kiss', 'cmd_pull', 'cmd_push');

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.keys.cmd_click_invisible_tiles', 'cinvisibili'),
    ('commands.description.cmd_click_invisible_tiles', ':cinvisibili - clicca tutti i Wired invisibili nella stanza (solo proprietario).'),
    ('commands.success.cmd_click_invisible_tiles', 'Wired invisibili cliccati: %count%.'),
    ('commands.keys.cmd_kiss', 'kiss;bacio'),
    ('commands.description.cmd_kiss', ':bacio <utente>'),
    ('commands.error.cmd_kiss.usage', 'Utilizzo: :bacio <utente>'),
    ('commands.action.kiss.sender', '*Ho baciato %target%*'),
    ('commands.action.kiss.receiver', '*Ho ricevuto un bacio da %sender%*'),
    ('commands.keys.cmd_punch', 'pugno;punch'),
    ('commands.description.cmd_punch', ':pugno <utente>'),
    ('commands.error.cmd_punch.usage', 'Utilizzo: :pugno <utente>'),
    ('commands.action.punch.sender', '*Ho dato un pugno a %target%*'),
    ('commands.action.punch.receiver', '*Ho ricevuto un pugno da %sender%*'),
    ('commands.error.target_not_found', 'Utente %user% non trovato nella stanza.'),
    ('commands.error.target_self', 'Non puoi usare questo comando su te stesso.'),
    ('commands.error.room_owner_only', 'Solo il proprietario della stanza puo usare questo comando.'),
    ('commands.error.cmd_pull.disabled', 'Il pull e disattivato in questa stanza. Il proprietario puo abilitarlo con :togglepull.'),
    ('commands.error.cmd_push.disabled', 'Il push e disattivato in questa stanza. Il proprietario puo abilitarlo con :togglepush.'),
    ('commands.keys.cmd_toggle_pull', 'togglepull'),
    ('commands.description.cmd_toggle_pull', ':togglepull - abilita/disabilita :pull nella stanza.'),
    ('commands.success.cmd_toggle_pull.enabled', 'Pull abilitato nella stanza.'),
    ('commands.success.cmd_toggle_pull.disabled', 'Pull disabilitato nella stanza.'),
    ('commands.keys.cmd_toggle_push', 'togglepush'),
    ('commands.description.cmd_toggle_push', ':togglepush - abilita/disabilita :push nella stanza.'),
    ('commands.success.cmd_toggle_push.enabled', 'Push abilitato nella stanza.'),
    ('commands.success.cmd_toggle_push.disabled', 'Push disabilitato nella stanza.'),
    ('commands.keys.cmd_toggle_trade', 'toggletrade'),
    ('commands.description.cmd_toggle_trade', ':toggletrade - abilita/disabilita gli scambi nella stanza.'),
    ('commands.success.cmd_toggle_trade.enabled', 'Scambi abilitati nella stanza.'),
    ('commands.success.cmd_toggle_trade.disabled', 'Scambi disabilitati nella stanza.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

INSERT INTO `emulator_settings` (`key`, `value`) VALUES
    ('camera.url', 'http://127.0.0.1:8080/camera/'),
    ('imager.location.output.camera', 'C:/Users/xAstroBoy/Desktop/Custom Habbo Retro/AtomCMS/public/camera/')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

UPDATE `items`
SET `extra_data`=REPLACE(
        REPLACE(
            REPLACE(`extra_data`, 'https://photo.bsshotel.it//', 'http://127.0.0.1:8080/camera/'),
            'https://photo.bsshotel.it/', 'http://127.0.0.1:8080/camera/'),
        'http://yourdomain.com/camera/', 'http://127.0.0.1:8080/camera/')
WHERE LOWER(`extra_data`) LIKE '%photo.bsshotel.it%'
   OR LOWER(`extra_data`) LIKE '%yourdomain.com/camera/%';

UPDATE `items`
SET `extra_data`=REPLACE(`extra_data`, '"u":"%id%"', CONCAT('"u":"', `id`, '"'))
WHERE `extra_data` LIKE '%"u":"%id%"%';
