-- A previous Hubbix pet migration reused the existing Triangular Prism page ID.
-- Its furniture offers were preserved, but the page metadata was left with the
-- "pets" layout. Nitro consequently tried to parse ordinary furniture offers
-- as pet offers and the page could no longer be opened.
DELETE ci
FROM `catalog_items` ci
JOIN `items_base` ib ON ib.`id` = CAST(ci.`item_ids` AS UNSIGNED)
WHERE ci.`page_id` = 2146999978
  AND ci.`catalog_name` LIKE 'Hubbix Pet %'
  AND ib.`item_name` REGEXP '^a0 pet[0-9]+$';

UPDATE `catalog_pages`
SET `parent_id` = 2145999957,
    `caption_save` = 'triangular_prism',
    `caption` = 'Triangular Prism',
    `page_layout` = 'default_3x3',
    `icon_color` = 1,
    `icon_image` = 28,
    `min_rank` = 1,
    `order_num` = 13,
    `visible` = '1',
    `enabled` = '1',
    `club_only` = '0',
    `catalog_mode` = 'NORMAL',
    `vip_only` = '0',
    `page_headline` = 'block_header',
    `page_teaser` = 'teaser_blocks',
    `page_special` = '',
    `page_text1` = '1 Block... 2 Block... Red Block... Blue Block',
    `page_text2` = NULL,
    `page_text_details` = '',
    `page_text_teaser` = ''
WHERE `id` = 2146999978;

-- Keep the replacement pet page visible and on the pet-specific layout.
UPDATE `catalog_pages`
SET `parent_id` = 2145999946,
    `caption_save` = 'hubbix_pets',
    `caption` = 'Tutti i Cuccioli',
    `page_layout` = 'pets',
    `visible` = '1',
    `enabled` = '1'
WHERE `id` = 2147000002;
