-- BSS room bundles can contain more than fifty products.  666 characters
-- truncates valid item_ids payloads, so keep live, BC, and versioned catalogs aligned.
ALTER TABLE `catalog_items`
    MODIFY COLUMN `item_ids` VARCHAR(2048) NOT NULL;

ALTER TABLE `catalog_items_bc`
    MODIFY COLUMN `item_ids` VARCHAR(2048) NOT NULL;

ALTER TABLE `catalog_version_offers`
    MODIFY COLUMN `item_ids` VARCHAR(2048) NOT NULL;
