-- Make clicking the WIRED root itself useful: show the former "Top Picks"
-- offers directly while keeping every other WIRED category as a child.
UPDATE `catalog_items`
SET `page_id` = 1800
WHERE `page_id` = 617;

UPDATE `catalog_pages`
SET `visible` = '0', `enabled` = '0'
WHERE `id` = 617;

-- The source text contained two zero-width spaces which are not representable
-- by the legacy emulator_texts character set.
INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.errors.cmd_hidewired.permission', 'Non hai il permesso di nascondere i WIRED in questa stanza!'),
    ('commands.description.cmd_commands', ':help [pagina] - Mostra i comandi disponibili per il tuo livello.')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
