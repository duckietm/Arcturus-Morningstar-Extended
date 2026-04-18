SET FOREIGN_KEY_CHECKS = 0;
SET @OLD_SQL_MODE = @@SQL_MODE;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

ALTER TABLE IF EXISTS `achievements` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `items` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `rooms` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `users_achievements` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `catalog_items` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `catalog_pages` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `navigator_flatcats` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `wordfilter` ENGINE = InnoDB, ROW_FORMAT = DYNAMIC;
ALTER TABLE IF EXISTS `logs_shop_purchases` MODIFY `user_id` int(11) DEFAULT NULL;
ALTER TABLE IF EXISTS `users_subscriptions` MODIFY `user_id` int(11) DEFAULT NULL;
ALTER TABLE IF EXISTS `items` MODIFY `item_id` int(11) unsigned DEFAULT 0;


DELIMITER //
DROP PROCEDURE IF EXISTS `_add_id_pk_if_missing`//
CREATE PROCEDURE `_add_id_pk_if_missing`(IN tbl VARCHAR(64))
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl AND `COLUMN_NAME` = 'id';
    IF col_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL `_add_id_pk_if_missing`('bot_serves');
CALL `_add_id_pk_if_missing`('chatlogs_room');
CALL `_add_id_pk_if_missing`('commandlogs');
CALL `_add_id_pk_if_missing`('room_enter_log');

DELIMITER //
DROP PROCEDURE IF EXISTS `_add_index_if_missing`//
CREATE PROCEDURE `_add_index_if_missing`(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(255))
BEGIN
    DECLARE tbl_exists INT DEFAULT 0;
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO tbl_exists FROM `information_schema`.`TABLES` WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl;
    IF tbl_exists > 0 THEN
        SELECT COUNT(*) INTO idx_exists FROM `information_schema`.`STATISTICS` WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl AND `INDEX_NAME` = idx;
        IF idx_exists = 0 THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD INDEX `', idx, '` (', cols, ')');
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END//
DELIMITER ;

CALL `_add_index_if_missing`('bans', 'idx_bans_user_id', '`user_id`');
CALL `_add_index_if_missing`('guilds', 'idx_guilds_user_id', '`user_id`');
CALL `_add_index_if_missing`('room_bans', 'idx_room_bans_room_id', '`room_id`');


DELIMITER //
DROP PROCEDURE IF EXISTS `_add_fk_if_missing`//
CREATE PROCEDURE `_add_fk_if_missing`(IN tbl VARCHAR(64), IN fk_name VARCHAR(64), IN col VARCHAR(64), IN ref_tbl VARCHAR(64), IN ref_col VARCHAR(64), IN on_del VARCHAR(20))
BEGIN
    DECLARE tbl_exists INT DEFAULT 0;
    DECLARE ref_exists INT DEFAULT 0;
    DECLARE fk_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO tbl_exists FROM `information_schema`.`TABLES` WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl;
    SELECT COUNT(*) INTO ref_exists FROM `information_schema`.`TABLES` WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = ref_tbl;
    
    IF tbl_exists > 0 AND ref_exists > 0 THEN
        SELECT COUNT(*) INTO fk_exists FROM `information_schema`.`TABLE_CONSTRAINTS` WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = tbl AND `CONSTRAINT_NAME` = fk_name;
        IF fk_exists = 0 THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD CONSTRAINT `', fk_name, '` FOREIGN KEY (`', col, '`) REFERENCES `', ref_tbl, '` (`', ref_col, '`) ON DELETE ', on_del);
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END//
DELIMITER ;

CALL `_add_fk_if_missing`('rooms', 'fk_rooms_owner', 'owner_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('items', 'fk_items_user', 'user_id', 'users', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('catalog_items', 'fk_catitems_page', 'page_id', 'catalog_pages', 'id', 'CASCADE');
CALL `_add_fk_if_missing`('guilds', 'fk_guilds_user', 'user_id', 'users', 'id', 'CASCADE');

DROP PROCEDURE IF EXISTS `_add_id_pk_if_missing`;
DROP PROCEDURE IF EXISTS `_add_index_if_missing`;
DROP PROCEDURE IF EXISTS `_add_fk_if_missing`;

# Make sure thenb System account exists
INSERT INTO `users` (`id`, `username`, `password`, `ip_register`, `ip_current`, `motto`, `look`, `rank`, `credits`)
SELECT 0, '[SYSTEM]', '!', '127.0.0.1', '127.0.0.1', 'System sentinel - do not delete', '', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `id` = 0);

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_MODE = @OLD_SQL_MODE;