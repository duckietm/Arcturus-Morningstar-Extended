-- Fortune wheel: prize weights now carry two decimals in the admin UI by
-- storing hundredths ("basis points") in the existing integer column.
-- Scaling every stored weight by 100 keeps each prize's relative
-- probability exactly the same.
UPDATE `wheel_prizes` SET `weight` = `weight` * 100;

-- :invsee — staff inventory inspection command (texts in Italian).
INSERT INTO `emulator_texts` (`key`, `value`) VALUES
	('commands.keys.cmd_invsee', 'invsee'),
	('commands.description.cmd_invsee', ':invsee <utente>'),
	('commands.generic.cmd_invsee.usage', 'Uso: :invsee <utente>'),
	('commands.generic.cmd_invsee.not_found', 'Utente %user% non trovato.')
ON DUPLICATE KEY UPDATE `value` = `value`;

-- Permission: same ranks that can already manage the fortune wheel.
INSERT IGNORE INTO `permission_definitions` (`permission_key`, `max_value`, `comment`)
VALUES (
	'cmd_invsee',
	1,
	'Staff: :invsee <user> opens a read/confiscate view of another user''s inventory (InvseeView). Gates the command and both invsee packets.'
);

SET @cols := NULL;
SELECT GROUP_CONCAT(CONCAT('dst.`', `column_name`, '` = src.`', `column_name`, '`') SEPARATOR ', ')
    INTO @cols
FROM `information_schema`.`columns`
WHERE `table_schema` = DATABASE()
  AND `table_name`   = 'permission_definitions'
  AND `column_name`  REGEXP '^rank_[0-9]+$';

SET @sql := CONCAT(
    'UPDATE `permission_definitions` dst ',
    'JOIN `permission_definitions` src ON src.`permission_key` = ''acc_wheeladmin'' ',
    'SET ', @cols, ' ',
    'WHERE dst.`permission_key` = ''cmd_invsee'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
