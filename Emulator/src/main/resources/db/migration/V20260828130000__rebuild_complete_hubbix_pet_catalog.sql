-- The earlier Hubbix pet migration accidentally reused the existing
-- Triangular Prism page ID. Keep that unrelated page intact and rebuild the
-- pet catalogue on a dedicated child page using every valid pet interaction.
INSERT INTO `catalog_pages`
    (`id`,`parent_id`,`caption_save`,`caption`,`page_layout`,`icon_color`,`icon_image`,`min_rank`,`order_num`,`visible`,`enabled`,`club_only`,`catalog_mode`,`vip_only`,`page_headline`,`page_teaser`,`page_special`,`page_text1`,`page_text2`,`page_text_details`,`page_text_teaser`,`room_id`,`includes`)
VALUES
    (2147000002,2145999946,'hubbix_pets','Tutti i Cuccioli','pets',1,166,1,99,'1','1','0','NORMAL','0','catalog_pet_headline1','','','Scegli il tuo nuovo animale.','Nome del pet:','Scegli colore e razza:',NULL,0,'')
ON DUPLICATE KEY UPDATE `caption`=VALUES(`caption`),`page_layout`=VALUES(`page_layout`),`parent_id`=VALUES(`parent_id`),`visible`='1',`enabled`='1';

-- This is a generated catalogue page: remove only its previous entries,
-- never offers belonging to another catalogue page.
DELETE FROM `catalog_items` WHERE `page_id`=2147000002;

INSERT INTO `catalog_items`
    (`item_ids`,`page_id`,`catalog_name`,`cost_credits`,`cost_points`,`points_type`,`amount`,`limited_stack`,`limited_sells`,`order_number`,`offer_id`,`song_id`,`extradata`,`have_offer`,`club_only`)
SELECT CAST(`id` AS CHAR),2147000002,
       CONCAT('Cucciolo ', REPLACE(TRIM(`interaction_type`),'pet','')),
       3,0,0,1,0,0,`id`,-1,0,'','1','0'
FROM `items_base`
WHERE TRIM(`interaction_type`) REGEXP '^pet[0-9]+$'
ORDER BY `id`;
