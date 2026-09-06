INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.description.cmd_click_invisible_tiles', ':cinvisibili - clicca tutti i furni delle categorie Invisibili presenti nella stanza.'),
    ('commands.success.cmd_click_invisible_tiles', 'Furni delle categorie Invisibili cliccati: %count%.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
