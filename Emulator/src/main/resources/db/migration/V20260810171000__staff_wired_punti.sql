-- The two imported BSS currency effects belong in Staff Wired and must use
-- real emulator interactions instead of the inert default interaction.
UPDATE `items_base`
SET `public_name`='WIRED EFFETTO: Invia Diamanti',
    `interaction_type`='wf_act_give_diamonds',
    `interaction_modes_count`=5
WHERE `id`=2000041961;

UPDATE `items_base`
SET `public_name`='WIRED EFFETTO: Invia Punti',
    `interaction_type`='wf_act_give_bss_points',
    `interaction_modes_count`=5
WHERE `id`=2000041962;

UPDATE `catalog_items`
SET `page_id`=255,
    `catalog_name`='WIRED EFFETTO: Invia Diamanti',
    `order_number`=9000
WHERE `id`=2137003172 AND `item_ids`='2000041961';

UPDATE `catalog_items`
SET `page_id`=255,
    `catalog_name`='WIRED EFFETTO: Invia Punti',
    `order_number`=9001
WHERE `id`=2137003171 AND `item_ids`='2000041962';

-- Keep the source BSS viewer leaf count honest after moving both offers.
UPDATE `catalog_pages` AS page
SET page.`caption` = REGEXP_REPLACE(
        page.`caption`,
        '\\([0-9]+\\)$',
        CONCAT('(', (SELECT COUNT(*) FROM `catalog_items` AS item WHERE item.`page_id`=page.`id`), ')'))
WHERE page.`id`=2139200000;

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('seasonal.name.103', 'Punti'),
    ('commands.keys.cmd_points', 'points;diamonds;givepoints'),
    ('commands.description.cmd_points', ':points <utente> <quantità> [tipo] | :givepoints <utente> <quantità>')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
