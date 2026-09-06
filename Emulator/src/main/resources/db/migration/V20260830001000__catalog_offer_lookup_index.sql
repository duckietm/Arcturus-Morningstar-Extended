-- Used by global catalog search, purchases and Hubbix reconciliation.
SET @offer_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'catalog_items'
      AND index_name = 'idx_catalog_items_offer_id'
);
SET @offer_index_sql = IF(
    @offer_index_exists = 0,
    'ALTER TABLE `catalog_items` ADD INDEX `idx_catalog_items_offer_id` (`offer_id`)',
    'SELECT 1'
);
PREPARE offer_index_statement FROM @offer_index_sql;
EXECUTE offer_index_statement;
DEALLOCATE PREPARE offer_index_statement;
