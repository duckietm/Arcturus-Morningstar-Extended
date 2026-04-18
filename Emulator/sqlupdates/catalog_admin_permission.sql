-- ============================================================
-- Catalog & Furni Admin Permission
-- Adds acc_catalogfurni permission to the permissions table
-- Required by: CatalogAdmin packet handlers (10050-10059)
-- ============================================================

-- 1. Add the column to the permissions table
ALTER TABLE `permissions`
    ADD COLUMN `acc_catalogfurni` ENUM('0','1') NOT NULL DEFAULT '0'
    AFTER `acc_catalog_ids`;

-- 2. Enable for Administrator (rank 7) by default
UPDATE `permissions` SET `acc_catalogfurni` = '1' WHERE `id` = 7;

-- Optional: enable for other ranks as needed
-- UPDATE `permissions` SET `acc_catalogfurni` = '1' WHERE `id` = 6; -- Super Mod
-- UPDATE `permissions` SET `acc_catalogfurni` = '1' WHERE `id` = 5; -- Moderator
