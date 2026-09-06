-- Collapse the legacy duplicate Furni root into the active imported Furni tree.
UPDATE `catalog_pages`
SET `parent_id` = 1701
WHERE `parent_id` = 2
  AND `id` <> 218;

UPDATE `catalog_pages`
SET `visible` = '0', `enabled` = '0'
WHERE `id` = 2;

-- The active WIRED root contained a second empty Wired wrapper. Move every
-- actual WIRED category one level up so Nitro can render them immediately.
UPDATE `catalog_pages`
SET `parent_id` = 1800
WHERE `parent_id` = 218;

UPDATE `catalog_pages`
SET `visible` = '0', `enabled` = '0'
WHERE `id` = 218;

UPDATE `catalog_pages`
SET `caption` = CASE `id`
    WHEN 1800 THEN 'WIRED'
    WHEN 617 THEN 'In evidenza'
    WHEN 618 THEN 'Offerte'
    WHEN 63 THEN 'Trigger'
    WHEN 64 THEN 'Effetti'
    WHEN 65 THEN 'Condizioni'
    WHEN 211 THEN 'Componenti aggiuntivi'
    WHEN 421 THEN 'Effetti sonori'
    WHEN 256 THEN 'Classifiche'
    WHEN 619 THEN 'Come usare WIRED'
    WHEN 900100 THEN 'WIRED V2 - Trigger'
    WHEN 900101 THEN 'WIRED V2 - Effetti 1/3'
    WHEN 900102 THEN 'WIRED V2 - Effetti 2/3'
    WHEN 900103 THEN 'WIRED V2 - Effetti 3/3'
    WHEN 900104 THEN 'WIRED V2 - Condizioni 1/2'
    WHEN 900105 THEN 'WIRED V2 - Condizioni 2/2'
    WHEN 900106 THEN 'WIRED V2 - Selettori'
    WHEN 900107 THEN 'WIRED V2 - Variabili'
    WHEN 900108 THEN 'WIRED V2 - Componenti aggiuntivi'
    WHEN 900109 THEN 'WIRED V2 - Memoria'
    ELSE `caption`
END
WHERE `id` IN (1800,617,618,63,64,65,211,421,256,619,
               900100,900101,900102,900103,900104,900105,
               900106,900107,900108,900109);

-- The BSS floor-rares import was paginated, but its parent had no offers.
-- Put the first 200 offers on the clickable parent and retain the remaining
-- 39 subpages. No offers are duplicated or deleted.
UPDATE `catalog_items`
SET `page_id` = 2145999940
WHERE `page_id` = 2144999999;

DELETE FROM `catalog_pages`
WHERE `id` = 2144999999;

UPDATE `catalog_pages`
SET `caption` = 'Rari BSS da pavimento',
    `page_layout` = 'default_3x3'
WHERE `id` = 2145999940;

UPDATE `catalog_pages`
SET `caption` = REPLACE(`caption`, 'BSS Floor Rares', 'Rari da pavimento')
WHERE `parent_id` = 2145999940;
