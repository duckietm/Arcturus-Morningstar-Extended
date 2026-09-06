-- Restore the colour-grouping page that was removed with the disabled Plasto
-- branch. It belongs directly under the active Furni root and is populated
-- from indexed Plasto variants, which enables Nitro's built-in colour picker.
INSERT INTO `catalog_pages`
    (`id`, `parent_id`, `caption_save`, `caption`, `page_layout`, `icon_color`, `icon_image`, `min_rank`, `order_num`, `visible`, `enabled`, `club_only`, `catalog_mode`, `vip_only`, `page_headline`, `page_teaser`, `page_special`, `page_text1`, `page_text2`, `page_text_details`, `page_text_teaser`, `room_id`, `includes`)
VALUES
    (19, 1701, 'colourable_plasto', 'Plasto Colorabile', 'default_3x3_color_grouping', 1, 46, 1, 1, '1', '1', '0', 'NORMAL', '0', 'plastic', 'plastic_pasic_promo_1', '', 'Scegli un colore per ogni mobile Plasto e acquistalo direttamente dal selettore.', '', '', '', 0, '')
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`), `caption_save` = VALUES(`caption_save`), `caption` = VALUES(`caption`), `page_layout` = VALUES(`page_layout`), `visible` = '1', `enabled` = '1';

INSERT INTO `catalog_items`
    (`item_ids`, `page_id`, `catalog_name`, `cost_credits`, `cost_points`, `points_type`, `amount`, `limited_stack`, `limited_sells`, `order_number`, `offer_id`, `song_id`, `extradata`, `have_offer`, `club_only`)
SELECT
    base.`id`, 19, base.`public_name`, IF(base.`item_name` LIKE 'chair_plasto%', 2, 5), 0, 0, 1, 0, 0, base.`id`, -1, 0, '', '1', '0'
FROM `items_base` AS base
WHERE base.`item_name` REGEXP '^(table_plasto_4leg|table_plasto_bigsquare|table_plasto_round|table_plasto_square|chair_plasto)\\*[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM `catalog_items` AS offer WHERE offer.`page_id` = 19 AND offer.`item_ids` = CAST(base.`id` AS CHAR)
  );
