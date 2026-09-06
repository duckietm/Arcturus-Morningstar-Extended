-- Keep the complete BSS badge-display collection on its purpose-built page.
UPDATE catalog_pages
SET caption = 'Vetrine Distintivi',
    page_layout = 'badge_display',
    page_headline = 'catalog_header_badgedisplay',
    page_teaser = 'badgedisplay_promo2',
    page_text1 = 'Hai lavorato per ottenere i tuoi Distintivi: scegli una vetrina e mostra in stanza il tuo preferito.',
    visible = '1',
    enabled = '1'
WHERE id = 52;

-- These are duplicate copies of the same 18 BSS holders already present on
-- page 52. Removing only this duplicate set leaves unrelated rares untouched.
DELETE FROM catalog_items
WHERE page_id = 2144999999
  AND CAST(
        SUBSTRING_INDEX(
            SUBSTRING_INDEX(TRIM(item_ids), ';', 1),
            ':',
            1
        ) AS UNSIGNED
      ) IN
      (5013,5014,5015,5016,5017,43371341,43371342,43371343,43371344,
       43371345,43371346,43371348,43371349,43371350,89319126,89319129,
       2000036302,2000036303);

-- Explicit public colour collections, including Plasto.
UPDATE catalog_pages
SET page_layout = 'default_3x3_color_grouping'
WHERE id IN
      (36,3041,3185,3304,9127,901015,934859451,89292928,392838233,
       934859519,934859625);

-- Imported pages made entirely from *colour variants should use the colour
-- grouping renderer too. Require at least two valid offers so ordinary pages
-- containing a lone suffixed item are not reclassified.
UPDATE catalog_pages page
SET page.page_layout = 'default_3x3_color_grouping'
WHERE page.visible = '1'
  AND page.enabled = '1'
  AND (
      SELECT COUNT(*)
      FROM catalog_items item
      WHERE item.page_id = page.id
  ) >= 2
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      LEFT JOIN items_base base
        ON base.id = CAST(
            SUBSTRING_INDEX(
                SUBSTRING_INDEX(TRIM(item.item_ids), ';', 1),
                ':',
                1
            ) AS UNSIGNED
        )
      WHERE item.page_id = page.id
        AND (
            base.id IS NULL
            OR base.item_name NOT LIKE '%*%'
        )
  );