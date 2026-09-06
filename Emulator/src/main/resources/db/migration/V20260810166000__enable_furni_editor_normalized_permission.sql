-- Polaris reads permission flags from permission_definitions when the
-- normalized schema is present. Keep the Administrator grant in sync with
-- the legacy permissions column updated by the preceding migration.
UPDATE `permission_definitions`
SET `rank_7` = 1
WHERE `permission_key` = 'acc_catalogfurni'
  AND `rank_7` <> 1;
