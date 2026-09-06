-- Members may reach the command handler only as room owners. The Java guard
-- additionally excludes ordinary rights holders; ranks 4+ remain staff-enabled.
UPDATE `permission_definitions`
SET `rank_1` = 2,
    `rank_2` = 2,
    `rank_3` = 2,
    `rank_4` = 1,
    `rank_5` = 1,
    `rank_6` = 1,
    `rank_7` = 1,
    `comment` = 'Eventi stanza: proprietario della stanza o staff; bersaglio facoltativo.'
WHERE `permission_key` = 'cmd_trash';

UPDATE `permissions`
SET `cmd_trash` = CASE WHEN `level` >= 4 THEN '1' ELSE '2' END;

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.description.cmd_trash', ':tornado [utente] / :sharknado [utente] - colpisce te o il giocatore scelto (proprietario stanza o staff).'),
    ('commands.error.cmd_fun_room.permission', 'Solo il proprietario della stanza o lo staff può usare questo comando.'),
    ('commands.error.cmd_trash.already_active', 'C’è già un evento in corso in questa stanza.'),
    ('commands.error.cmd_trash.unavailable', 'Sharknado non disponibile: mancano gli squali o non c’è abbastanza spazio.'),
    ('commands.action.cmd_trash.warning', 'Sharknado in arrivo! Gli squali stanno volando intorno a te!'),
    ('commands.action.cmd_trash.finished', 'Lo Sharknado è finito: sei tornato sano e salvo!'),
    ('commands.error.cmd_tornado.unavailable', 'Tornado non disponibile: non c’è abbastanza spazio nella stanza.'),
    ('commands.action.cmd_tornado.warning', 'Il tornado ti ha catturato! Tieniti forte!'),
    ('commands.action.cmd_tornado.finished', 'Il tornado è finito: tu e tutti i furni siete tornati al vostro posto.'),
    ('commands.help.cmd_fun_room', 'EVENTI STANZA (proprietario o staff): :disco, :rave, :terremoto, :cannoni, :levitazione, :girotondo, :invasionefufo, :piratiparty, :ghostparty, :robotparty, :pioggiadisco, :caos, :statue, :carnevale.\r\nATMOSFERE: :spazio, :aurora, :temporale, :tramonto, :neonvoid, :blackout, :oceano, :inferno.\r\nSU UN UTENTE: :rapisci, :rimbalza, :vola, :orbita, :frullatore, :yoyo, :fantasma, :papera, :mummia, :zombie, :goblin, :alieno <utente>.\r\nINTERAZIONI: :scambio, :calamita, :duello, :abbraccio, :telepatia, :catapulta <utente>.\r\nCATASTROFI: :tornado [utente], :sharknado [utente].'),
    ('commands.error.cmd_fun_room.active', 'C’è già un evento attivo in questa stanza.'),
    ('commands.error.cmd_fun_room.usage', 'Uso corretto: :%command% <utente>'),
    ('commands.action.cmd_fun_room.started', 'Evento “%event%” avviato!'),
    ('commands.action.cmd_fun_room.finished', 'Evento terminato: tutto è stato ripristinato.'),
    ('commands.action.cmd_fun_target.started', 'Sei stato colpito dall’evento “%event%”!'),
    ('commands.action.cmd_fun_pair.scambio', '%actor% e %target% si scambiano di posto!'),
    ('commands.action.cmd_fun_pair.calamita', '%actor% attira %target% come una calamita!'),
    ('commands.action.cmd_fun_pair.duello', '%actor% sfida %target% a duello!'),
    ('commands.action.cmd_fun_pair.abbraccio', '%actor% abbraccia %target%!'),
    ('commands.action.cmd_fun_pair.telepatia', '%actor% e %target% comunicano con la mente!'),
    ('commands.action.cmd_fun_pair.catapulta', '%actor% catapulta %target% attraverso la stanza!')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
