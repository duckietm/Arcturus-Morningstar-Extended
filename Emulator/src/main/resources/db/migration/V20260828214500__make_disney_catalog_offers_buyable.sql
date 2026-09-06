-- Keep every imported Hubbis Disney furni on the actual Disney subpage and
-- make it purchasable with the hotel's default credit price.
UPDATE catalog_items
SET page_id = 2138999134,
    cost_credits = 3,
    cost_points = 0,
    points_type = 0,
    amount = GREATEST(amount, 1),
    have_offer = '1'
WHERE catalog_name LIKE 'bsstonino_disney%';

-- Retried bulk operations from older clients could create multiple offers for
-- the same base furni. Keep the stable first offer so the catalogue grid and
-- purchase resolver expose exactly one product per Disney furni.
DELETE duplicate_offer
FROM catalog_items duplicate_offer
JOIN (
    SELECT item_ids, catalog_name, MIN(id) AS keep_id
    FROM catalog_items
    WHERE catalog_name LIKE 'bsstonino_disney%'
    GROUP BY item_ids, catalog_name
) canonical
  ON canonical.item_ids = duplicate_offer.item_ids
 AND canonical.catalog_name = duplicate_offer.catalog_name
WHERE duplicate_offer.catalog_name LIKE 'bsstonino_disney%'
  AND duplicate_offer.id <> canonical.keep_id;
