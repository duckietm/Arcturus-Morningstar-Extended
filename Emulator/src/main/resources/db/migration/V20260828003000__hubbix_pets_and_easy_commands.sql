-- A stable, normal-player pet page backed by the published Hubbix pet assets.
INSERT INTO `catalog_pages`
    (`id`,`parent_id`,`caption_save`,`caption`,`page_layout`,`icon_color`,`icon_image`,`min_rank`,`order_num`,`visible`,`enabled`,`club_only`,`catalog_mode`,`vip_only`,`page_headline`,`page_teaser`,`page_special`,`page_text1`,`page_text2`,`page_text_details`,`page_text_teaser`,`room_id`,`includes`)
VALUES
    (2146999978,2145999946,'hubbix_pets','Hubbix Pets','pets',1,166,1,99,'1','1','0','NORMAL','0','catalog_pet_headline1','','','Scegli il tuo nuovo animale. Tutti i pet hanno subito tutti i comandi disponibili.','Nome del pet:','Scegli colore e razza:',NULL,0,'')
ON DUPLICATE KEY UPDATE `caption`=VALUES(`caption`),`page_layout`=VALUES(`page_layout`),`visible`='1',`enabled`='1';

INSERT INTO `catalog_items`
    (`item_ids`,`page_id`,`catalog_name`,`cost_credits`,`cost_points`,`points_type`,`amount`,`limited_stack`,`limited_sells`,`order_number`,`offer_id`,`song_id`,`extradata`,`have_offer`,`club_only`)
SELECT CAST(`id` AS CHAR),2146999978,CONCAT('Hubbix Pet ', `id`),10,0,0,1,0,0,`id`,-1,0,'','1','0'
FROM `items_base`
WHERE `interaction_type` REGEXP '^pet[0-9]+'
  AND `id` BETWEEN 50000 AND 50037
  AND NOT EXISTS (SELECT 1 FROM `catalog_items` ci WHERE ci.`page_id`=2146999978 AND ci.`item_ids`=CAST(`items_base`.`id` AS CHAR));

UPDATE `emulator_texts` SET `value`='handitem;ha;handitem all' WHERE `key`='commands.keys.cmd_hand_item';
UPDATE `emulator_texts` SET `value`='roomeffect;eff' WHERE `key`='commands.keys.cmd_roomeffect';
UPDATE `emulator_texts` SET `value`=CONCAT(`value`, ';massdiamond') WHERE `key`='commands.keys.cmd_masspoints' AND `value` NOT LIKE '%massdiamond%';
UPDATE `emulator_texts` SET `value`=CONCAT(`value`, ';achievement;achievements') WHERE `key`='commands.keys.cmd_update_achievements' AND `value` NOT LIKE '%achievement;%';

INSERT INTO `emulator_texts` (`key`,`value`) VALUES
    ('commands.description.cmd_hand_item', ':handitem <id> | :handitem all <id> - assegna un handitem a te o a tutta la stanza.'),
    ('commands.description.cmd_roomeffect', ':roomeffect <id> | :eff room <id> - applica un effetto a tutta la stanza.'),
    ('commands.description.cmd_masspoints', ':masspoints <quantita> [valuta] | :massdiamond <quantita> - accredita solo gli utenti online.'),
    ('commands.description.cmd_update_achievements', ':achievements list | :achievement set <utente> <achievement|maxall> <livello>.'),
    ('commands.description.cmd_pet_info', ':petinfo <petname> | :maxpetstat <petname> - potenzia il tuo pet presente nella stanza.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
