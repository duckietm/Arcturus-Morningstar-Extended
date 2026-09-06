-- Plasto variants need Nitro's indexed-colour grouping renderer.
UPDATE catalog_pages
SET page_layout = 'default_3x3_color_grouping'
WHERE id = 19
  AND caption = 'Plasto Colorabile';
