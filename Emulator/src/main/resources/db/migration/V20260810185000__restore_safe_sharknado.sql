INSERT INTO `emulator_texts` (`key`, `value`) VALUES
    ('commands.description.cmd_trash', ':sharknado - scatena una tempesta di squali nella stanza (solo staff).'),
    ('commands.action.cmd_trash.warning', '🌪️ SHARKNADO IN ARRIVO! Tieniti forte! 🦈'),
    ('commands.action.cmd_trash.finished', '🏴‍☠️ Sei sopravvissuto allo Sharknado!'),
    ('commands.error.cmd_trash.already_active', 'Uno Sharknado è già in corso in questa stanza.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
