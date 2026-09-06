-- Delete only invalid subpages. Repeating the delete also removes now-empty
-- branches and descendants of a broken parent; top-level catalogue tabs remain intact.
DROP PROCEDURE IF EXISTS `repair_catalogue_empty_subpages`;

DELIMITER $$
CREATE PROCEDURE `repair_catalogue_empty_subpages`()
BEGIN
    DECLARE deleted_rows INT DEFAULT 1;

    WHILE deleted_rows > 0 DO
        DELETE page
        FROM `catalog_pages` AS page
        LEFT JOIN `catalog_items` AS offer ON offer.`page_id` = page.`id`
        LEFT JOIN `catalog_pages` AS child ON child.`parent_id` = page.`id`
        WHERE page.`parent_id` > 0
          AND offer.`id` IS NULL
          AND child.`id` IS NULL;
        SET deleted_rows = ROW_COUNT();

        DELETE page
        FROM `catalog_pages` AS page
        LEFT JOIN `catalog_pages` AS parent ON parent.`id` = page.`parent_id`
        WHERE page.`parent_id` > 0
          AND parent.`id` IS NULL;
        SET deleted_rows = deleted_rows + ROW_COUNT();

        DELETE page
        FROM `catalog_pages` AS page
        JOIN `catalog_pages` AS parent ON parent.`id` = page.`parent_id`
        WHERE page.`parent_id` > 0
          AND page.`visible` = '1'
          AND page.`enabled` = '1'
          AND (parent.`visible` <> '1' OR parent.`enabled` <> '1');
        SET deleted_rows = deleted_rows + ROW_COUNT();
    END WHILE;
END$$
DELIMITER ;

CALL `repair_catalogue_empty_subpages`();

-- "Copertina" is the front-page tab (id 1), never a nested category.
DELETE FROM `catalog_pages`
WHERE `id` <> 1
  AND `parent_id` > 0
  AND (LOWER(TRIM(`caption`)) = 'copertina' OR LOWER(TRIM(`caption_save`)) LIKE '%copertina%');

CALL `repair_catalogue_empty_subpages`();
DROP PROCEDURE `repair_catalogue_empty_subpages`;
