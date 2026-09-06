-- Public BSS pages are authoritative: the same base furniture must not also
-- appear in the rank-7 Staff viewer.
DROP TEMPORARY TABLE IF EXISTS tmp_bss_public_item_ids;
CREATE TEMPORARY TABLE tmp_bss_public_item_ids (
    item_id INT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT IGNORE INTO tmp_bss_public_item_ids (item_id)
WITH RECURSIVE public_pages AS (
    SELECT id
    FROM catalog_pages
    WHERE id IN (1701, 1702, 1705, 1800)
    UNION ALL
    SELECT page.id
    FROM catalog_pages page
    JOIN public_pages parent ON page.parent_id = parent.id
), offer_tokens AS (
    SELECT
        item.id,
        SUBSTRING_INDEX(item.item_ids, ';', 1) AS token,
        CASE
            WHEN INSTR(item.item_ids, ';') = 0 THEN ''
            ELSE SUBSTRING(item.item_ids, INSTR(item.item_ids, ';') + 1)
        END AS rest
    FROM catalog_items item
    JOIN public_pages page ON page.id = item.page_id
    UNION ALL
    SELECT
        id,
        SUBSTRING_INDEX(rest, ';', 1),
        CASE
            WHEN INSTR(rest, ';') = 0 THEN ''
            ELSE SUBSTRING(rest, INSTR(rest, ';') + 1)
        END
    FROM offer_tokens
    WHERE rest <> ''
)
SELECT DISTINCT CAST(SUBSTRING_INDEX(token, ':', 1) AS UNSIGNED)
FROM offer_tokens
WHERE CAST(SUBSTRING_INDEX(token, ':', 1) AS UNSIGNED) > 0;

DROP TEMPORARY TABLE IF EXISTS tmp_bss_viewer_pages;
CREATE TEMPORARY TABLE tmp_bss_viewer_pages (
    page_id INT NOT NULL PRIMARY KEY
);

INSERT IGNORE INTO tmp_bss_viewer_pages (page_id)
WITH RECURSIVE viewer_pages AS (
    SELECT id
    FROM catalog_pages
    WHERE id = 2139999999
    UNION ALL
    SELECT page.id
    FROM catalog_pages page
    JOIN viewer_pages parent ON page.parent_id = parent.id
)
SELECT id FROM viewer_pages;

DELETE item
FROM catalog_items item
JOIN tmp_bss_viewer_pages viewer ON viewer.page_id = item.page_id
JOIN tmp_bss_public_item_ids public_item
    ON public_item.item_id = CAST(SUBSTRING_INDEX(item.item_ids, ':', 1) AS UNSIGNED)
WHERE item.item_ids NOT LIKE '%;%';

-- Remove viewer buckets which became genuinely empty after deduplication.
DELETE page
FROM catalog_pages page
LEFT JOIN catalog_items item ON item.page_id = page.id
LEFT JOIN catalog_pages child ON child.parent_id = page.id
WHERE page.parent_id IN (2139999999, 2145999940, 2145999939, 2145999938, 2145999937)
  AND item.id IS NULL
  AND child.id IS NULL;

-- Refresh every caption's embedded count from the live hierarchy as well.
DROP TEMPORARY TABLE IF EXISTS tmp_bss_viewer_counts;
CREATE TEMPORARY TABLE tmp_bss_viewer_counts (
    page_id INT NOT NULL PRIMARY KEY,
    offer_count INT NOT NULL
);

INSERT INTO tmp_bss_viewer_counts (page_id, offer_count)
WITH RECURSIVE viewer_pages AS (
    SELECT id
    FROM catalog_pages
    WHERE id = 2139999999
    UNION ALL
    SELECT page.id
    FROM catalog_pages page
    JOIN viewer_pages parent ON page.parent_id = parent.id
), page_closure AS (
    SELECT id AS ancestor_id, id AS descendant_id
    FROM viewer_pages
    UNION ALL
    SELECT closure.ancestor_id, page.id
    FROM page_closure closure
    JOIN catalog_pages page ON page.parent_id = closure.descendant_id
)
SELECT closure.ancestor_id, COUNT(item.id)
FROM page_closure closure
LEFT JOIN catalog_items item ON item.page_id = closure.descendant_id
GROUP BY closure.ancestor_id;

UPDATE catalog_pages page
JOIN tmp_bss_viewer_counts counts ON counts.page_id = page.id
SET page.caption = CASE
    WHEN page.caption REGEXP ' \\([0-9]+\\)$'
        THEN REGEXP_REPLACE(page.caption, ' \\([0-9]+\\)$', CONCAT(' (', counts.offer_count, ')'))
    ELSE CONCAT(page.caption, ' (', counts.offer_count, ')')
END;

DROP TEMPORARY TABLE IF EXISTS tmp_bss_viewer_counts;
DROP TEMPORARY TABLE IF EXISTS tmp_bss_viewer_pages;
DROP TEMPORARY TABLE IF EXISTS tmp_bss_public_item_ids;
