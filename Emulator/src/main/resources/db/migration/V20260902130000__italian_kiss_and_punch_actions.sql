INSERT INTO `emulator_texts` (`key`, `value`)
VALUES
    ('commands.action.kiss.sender', '*Dà un bacio a %target%*'),
    ('commands.action.kiss.receiver', '%sender% mi ha baciato :O'),
    ('commands.action.punch.sender', '*Dà un pugno a %target%*'),
    ('commands.action.punch.receiver', 'Ahi! %sender% mi ha dato un pugno!')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
