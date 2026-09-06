UPDATE `emulator_texts` SET `value`='tele;teleport;teletrasporto;tp' WHERE `key`='commands.keys.cmd_teleport';
UPDATE `emulator_texts` SET `value`='empty;pulisci;clearinventory' WHERE `key`='commands.keys.cmd_empty';
UPDATE `emulator_texts` SET `value`='invisible;hideme;invis' WHERE `key`='commands.keys.cmd_invisible';
UPDATE `emulator_texts` SET `value`='trash;tornado;sharknado' WHERE `key`='commands.keys.cmd_trash';

INSERT INTO `emulator_texts` (`key`,`value`) VALUES
    ('commands.description.cmd_teleport', ':teleport / :tp - attiva o disattiva il teletrasporto.'),
    ('commands.description.cmd_empty', ':pulisci si - svuota il tuo inventario.'),
    ('commands.description.cmd_invisible', ':invis hide | :invis show - nasconditi o torna visibile.'),
    ('commands.description.cmd_trash', ':sharknado <utente> - scatena squali volanti attorno al giocatore scelto.'),
    ('commands.description.cmd_badge', ':badge list - apre la libreria badge; :badge <utente> <codice> - assegna un badge.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
