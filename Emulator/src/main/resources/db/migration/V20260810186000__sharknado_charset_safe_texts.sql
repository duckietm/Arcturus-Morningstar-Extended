INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.description.cmd_trash', ':sharknado - scatena squali volanti animati nella stanza (solo staff).'),
    ('commands.action.cmd_trash.warning', 'SHARKNADO IN ARRIVO! Gli squali stanno volando!'),
    ('commands.action.cmd_trash.finished', 'Sei sopravvissuto allo Sharknado!'),
    ('commands.error.cmd_trash.already_active', 'Uno Sharknado e gia in corso in questa stanza.'),
    ('commands.error.cmd_trash.unavailable', 'Sharknado non disponibile: mancano squali o spazio nella stanza.'),
    ('commands.action.cmd_tornado.warning', 'Il tornado ti ha catturato! I furni stanno volando!'),
    ('commands.action.cmd_tornado.finished', 'Il tornado e finito: tu e tutti i furni siete tornati al vostro posto.'),
    ('commands.error.cmd_tornado.unavailable', 'Tornado non disponibile: la stanza e troppo piccola.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
