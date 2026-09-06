-- :invis controls the invisible-furniture command; avatar invisibility keeps its original aliases.
UPDATE `emulator_texts` SET `value`='invisible;hideme' WHERE `key`='commands.keys.cmd_invisible';
UPDATE `emulator_texts` SET `value`='cinvisibili;invis' WHERE `key`='commands.keys.cmd_click_invisible_tiles';

INSERT INTO `emulator_texts` (`key`,`value`) VALUES
    ('commands.description.cmd_click_invisible_tiles', ':cinvisibili - clicca tutti i furni Invisibili; :invis hide imposta stato 1, :invis show imposta stato 0.'),
    ('commands.description.cmd_transform', ':transform / :pet <animale> <razza> <colore>; :pet habbo torna normale.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
