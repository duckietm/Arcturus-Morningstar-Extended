CREATE TABLE IF NOT EXISTS `bss_user_preferences` (
    `user_id` INT NOT NULL,
    `do_not_disturb` TINYINT(1) NOT NULL DEFAULT 0,
    `block_gifts` TINYINT(1) NOT NULL DEFAULT 0,
    `block_whispers` TINYINT(1) NOT NULL DEFAULT 0,
    `block_mimic` TINYINT(1) NOT NULL DEFAULT 0,
    `block_kisses` TINYINT(1) NOT NULL DEFAULT 0,
    `group_chat_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `user_click_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `random_walk_priority` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `permission_definitions`
    (`permission_key`, `max_value`, `comment`, `rank_1`, `rank_2`, `rank_3`, `rank_4`, `rank_5`, `rank_6`, `rank_7`)
VALUES
    ('cmd_bss_dnd',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_block_gifts',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_block_whispers',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_block_mimic',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_block_kisses',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_group_chat',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_user_click',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_random_walk',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_kick_pets',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_kick_bots',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_regen_maps',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_close_dice',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_open_room',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_close_room',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_disable_effect',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_reload_credits',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_room_ban',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_toggle_pyramids',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_add_tag',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_remove_tag',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_clear_tags',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_trade',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_reset_prefix',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_report',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_clear_group_chat',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_convert_credits',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_convert_diamonds',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_rare_value',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_inventory_value',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_room_value',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_placex',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_force_height',1,'BSS command',1,1,1,1,1,1,1),
    ('cmd_bss_force_rotation',1,'BSS command',1,1,1,1,1,1,1)
ON DUPLICATE KEY UPDATE `rank_1`=1,`rank_2`=1,`rank_3`=1,`rank_4`=1,`rank_5`=1,`rank_6`=1,`rank_7`=1;

UPDATE `permission_definitions`
SET `rank_1`=1,`rank_2`=1,`rank_3`=1,`rank_4`=1,`rank_5`=1,`rank_6`=1,`rank_7`=1
WHERE `permission_key` IN (
    'cmd_blockalert','cmd_coords','cmd_diagonal','cmd_ejectall','cmd_empty','cmd_empty_bots',
    'cmd_empty_pets','cmd_enable','cmd_faceless','cmd_hand_item','cmd_hidewired','cmd_mimic',
    'cmd_moonwalk','cmd_pickall','cmd_reload_room','cmd_setmax','cmd_setspeed','cmd_stalk',
    'cmd_teleport','cmd_unload','cmd_transform','cmd_disablementions'
);

INSERT INTO `emulator_settings` (`key`,`value`) VALUES
    ('bss.commands.reload_credits.amount','5000')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

INSERT INTO `emulator_texts` (`key`,`value`) VALUES
    ('commands.keys.cmd_bss_dnd','dnd'),
    ('commands.keys.cmd_bss_block_gifts','bloccaregali'),
    ('commands.keys.cmd_bss_block_whispers','disablewhispers'),
    ('commands.keys.cmd_bss_block_mimic','disablemimic'),
    ('commands.keys.cmd_bss_block_kisses','disattivabacio'),
    ('commands.keys.cmd_bss_group_chat','disattivachatgruppo'),
    ('commands.keys.cmd_bss_user_click','tc'),
    ('commands.keys.cmd_bss_random_walk','camminatacasuale'),
    ('commands.keys.cmd_bss_kick_pets','cacciacuccioli'),
    ('commands.keys.cmd_bss_kick_bots','cacciabot'),
    ('commands.keys.cmd_bss_regen_maps','regenmaps'),
    ('commands.keys.cmd_bss_close_dice','chiudidadi'),
    ('commands.keys.cmd_bss_open_room','apristanza'),
    ('commands.keys.cmd_bss_close_room','chiudistanza'),
    ('commands.keys.cmd_bss_disable_effect','disattivaeffetto'),
    ('commands.keys.cmd_bss_reload_credits','ricaricami'),
    ('commands.keys.cmd_bss_room_ban','rban'),
    ('commands.keys.cmd_bss_toggle_pyramids','togglepyramide'),
    ('commands.keys.cmd_bss_add_tag','aggiungitag'),
    ('commands.keys.cmd_bss_remove_tag','rimuovitag'),
    ('commands.keys.cmd_bss_clear_tags','puliscitags'),
    ('commands.keys.cmd_bss_trade','scambia'),
    ('commands.keys.cmd_bss_reset_prefix','resetprefix'),
    ('commands.keys.cmd_bss_report','report'),
    ('commands.keys.cmd_bss_clear_group_chat','eliminachatgruppo'),
    ('commands.keys.cmd_bss_convert_credits','convertcredits'),
    ('commands.keys.cmd_bss_convert_diamonds','convertdiamonds'),
    ('commands.keys.cmd_bss_rare_value','valore'),
    ('commands.keys.cmd_bss_inventory_value','valori'),
    ('commands.keys.cmd_bss_room_value','valoristanza'),
    ('commands.keys.cmd_bss_placex','placex'),
    ('commands.keys.cmd_bss_force_height','forceheight'),
    ('commands.keys.cmd_bss_force_rotation','forcerot'),
    ('commands.error.room_owner_only','Solo il proprietario della stanza puo usare questo comando.'),
    ('commands.error.target_not_found','Utente %user% non trovato nella stanza.'),
    ('commands.error.target_self','Non puoi usare questo comando su te stesso.'),
    ('commands.error.cmd_kiss.blocked','Questo utente ha disattivato i baci.'),
    ('commands.error.cmd_bss_room_ban.usage','Utilizzo: :rban <utente>'),
    ('commands.error.cmd_bss_report.usage','Utilizzo: :report <utente> <motivo>'),
    ('commands.error.cmd_bss_report.pending','Hai gia una segnalazione in attesa.'),
    ('commands.error.cmd_bss_add_tag.usage','Utilizzo: :aggiungitag <tag>'),
    ('commands.error.cmd_bss_remove_tag.usage','Utilizzo: :rimuovitag <tag>'),
    ('commands.error.cmd_bss_rare_value.usage','Utilizzo: :valore <nome raro>'),
    ('commands.error.cmd_bss_rare_value.not_found','Raro non trovato: %name%.'),
    ('commands.error.cmd_bss_placex.usage','Utilizzo: :placex <numero> <altezza>'),
    ('commands.error.cmd_bss_force_height.usage','Utilizzo: :forceheight <altezza|off>'),
    ('commands.error.cmd_bss_force_rotation.usage','Utilizzo: :forcerot <0-7|off>'),
    ('commands.success.cmd_bss_placex','Impostata altezza per i prossimi furni.'),
    ('commands.success.cmd_bss_force_height','Altezza forzata attiva.'),
    ('commands.success.cmd_bss_force_height.disabled','Altezza forzata disattivata.'),
    ('commands.success.cmd_bss_force_rotation','Rotazione forzata attiva.'),
    ('commands.success.cmd_bss_force_rotation.disabled','Rotazione forzata disattivata.'),
    ('commands.success.cmd_bss_regen_maps','Mappa rigenerata: %count% caselle.'),
    ('commands.success.cmd_bss_reload_credits','Crediti ricaricati.'),
    ('commands.success.cmd_bss_report','Segnalazione inviata allo staff.'),
    ('commands.success.cmd_bss_clear_group_chat','Cronologia chat di gruppo rimossa dalla tua vista.'),
    ('commands.success.cmd_bss_convert_credits','Convertiti %items% furni in %amount% crediti.'),
    ('commands.success.cmd_bss_convert_diamonds','Convertiti %items% furni in %amount% diamanti.'),
    ('commands.generic.cmd_bss_rare_value','%name%\rCrediti: %credits%\r%type%: %points%'),
    ('commands.generic.cmd_bss_inventory_value','Furni valutati: %items%\rCrediti: %credits%\r%points%'),
    ('commands.generic.cmd_bss_room_value','Furni valutati: %items%\rCrediti: %credits%\r%points%')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

INSERT INTO `emulator_texts` (`key`,`value`) VALUES
    ('commands.success.cmd_bss_kick_pets','Cuccioli cacciati: %count%.'),
    ('commands.success.cmd_bss_kick_bots','Bot cacciati: %count%.'),
    ('commands.success.cmd_bss_close_dice','Dadi chiusi: %count%.'),
    ('commands.success.cmd_bss_open_room','Stanza aperta.'),
    ('commands.success.cmd_bss_close_room','Stanza chiusa.'),
    ('commands.success.cmd_bss_disable_effect','Effetto disattivato.'),
    ('commands.success.cmd_bss_reload_credits.changed','Crediti ricaricati: %credits%.'),
    ('commands.success.cmd_bss_reload_credits.unchanged','Hai ancora %credits% crediti.'),
    ('commands.success.cmd_bss_room_ban','%user% bannato dalla stanza per un ora.'),
    ('commands.success.cmd_bss_toggle_pyramids.visible','Piramidi Wired visibili.'),
    ('commands.success.cmd_bss_toggle_pyramids.hidden','Piramidi Wired nascoste.'),
    ('commands.success.cmd_bss_add_tag.changed','Tag aggiunto.'),
    ('commands.success.cmd_bss_add_tag.unchanged','Tag gia presente.'),
    ('commands.success.cmd_bss_remove_tag.changed','Tag rimosso.'),
    ('commands.success.cmd_bss_remove_tag.unchanged','Tag non presente.'),
    ('commands.success.cmd_bss_clear_tags.changed','Tag eliminati.'),
    ('commands.success.cmd_bss_clear_tags.unchanged','Non hai tag da eliminare.'),
    ('commands.success.cmd_bss_trade.enabled','Scambi personali abilitati.'),
    ('commands.success.cmd_bss_trade.disabled','Scambi personali disabilitati.'),
    ('commands.success.cmd_bss_reset_prefix.changed','Prefisso rimosso.'),
    ('commands.success.cmd_bss_reset_prefix.unchanged','Nessun prefisso attivo.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

INSERT INTO `emulator_texts` (`key`,`value`)
SELECT CONCAT('commands.description.', SUBSTRING(`key`, LENGTH('commands.keys.') + 1)),
       CONCAT(':', SUBSTRING_INDEX(`value`, ';', 1))
FROM `emulator_texts`
WHERE `key` LIKE 'commands.keys.cmd_bss_%'
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

-- Messaggi generici per i toggle e le azioni semplici.
INSERT INTO `emulator_texts` (`key`,`value`)
SELECT CONCAT('commands.success.', p.permission_key, '.enabled'), 'Funzione attivata.'
FROM `permission_definitions` p WHERE p.permission_key IN
('cmd_bss_dnd','cmd_bss_block_gifts','cmd_bss_block_whispers','cmd_bss_block_mimic','cmd_bss_block_kisses','cmd_bss_group_chat','cmd_bss_user_click','cmd_bss_random_walk')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
INSERT INTO `emulator_texts` (`key`,`value`)
SELECT CONCAT('commands.success.', p.permission_key, '.disabled'), 'Funzione disattivata.'
FROM `permission_definitions` p WHERE p.permission_key IN
('cmd_bss_dnd','cmd_bss_block_gifts','cmd_bss_block_whispers','cmd_bss_block_mimic','cmd_bss_block_kisses','cmd_bss_group_chat','cmd_bss_user_click','cmd_bss_random_walk')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

UPDATE `emulator_texts` SET `value`='lay;stenditi' WHERE `key`='commands.keys.cmd_lay';
UPDATE `emulator_texts` SET `value`='mutepets;ignorepets;mute_pets;ignore_pets;mutacuccioli' WHERE `key`='commands.keys.cmd_mute_pets';
UPDATE `emulator_texts` SET `value`='mutebots;ignorebots;mute_bots;ignore_bots;mutabot' WHERE `key`='commands.keys.cmd_mute_bots';
UPDATE `emulator_texts` SET `value`='mimic;copy;copialook' WHERE `key`='commands.keys.cmd_mimic';
UPDATE `emulator_texts` SET `value`='handitem;item;hand;mangia' WHERE `key`='commands.keys.cmd_hand_item';
UPDATE `emulator_texts` SET `value`='stalk;follow;rape;vai' WHERE `key`='commands.keys.cmd_stalk';
UPDATE `emulator_texts` SET `value`='faceless;face;toglifaccia' WHERE `key`='commands.keys.cmd_faceless';
UPDATE `emulator_texts` SET `value`='crash;unload;scarica' WHERE `key`='commands.keys.cmd_unload';
UPDATE `emulator_texts` SET `value`='empty;pulisci' WHERE `key`='commands.keys.cmd_empty';
UPDATE `emulator_texts` SET `value`='setmax;set_max;maxutenti' WHERE `key`='commands.keys.cmd_setmax';
UPDATE `emulator_texts` SET `value`='speed;setspeed;vroller' WHERE `key`='commands.keys.cmd_setspeed';
UPDATE `emulator_texts` SET `value`='diagonal;disablediagonal;diagonally;diagonali' WHERE `key`='commands.keys.cmd_diagonal';
UPDATE `emulator_texts` SET `value`='petinfo;pet_info' WHERE `key`='commands.keys.cmd_pet_info';
UPDATE `emulator_texts` SET `value`='transform;becomepet;pet' WHERE `key`='commands.keys.cmd_transform';
UPDATE `emulator_texts` SET `value`='tele;teleport;teletrasporto' WHERE `key`='commands.keys.cmd_teleport';
UPDATE `emulator_texts` SET `value`='reload_room;reload;reloadroom;ricarica' WHERE `key`='commands.keys.cmd_reload_room';
UPDATE `emulator_texts` SET `value`='blockalerts;blockalert;ignorealerts;ignore_alerts;disablealert' WHERE `key`='commands.keys.cmd_blockalert';
UPDATE `emulator_texts` SET `value`='emptypets;empty_pets;deletepets;puliscianimali' WHERE `key`='commands.keys.cmd_empty_pets';
UPDATE `emulator_texts` SET `value`='emptybots;empty_bots;deletebots;puliscibots' WHERE `key`='commands.keys.cmd_empty_bots';
