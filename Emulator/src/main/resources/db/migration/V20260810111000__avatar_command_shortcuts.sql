UPDATE emulator_texts
SET value = 'enable;effect;eff'
WHERE `key` = 'commands.keys.cmd_enable';

UPDATE emulator_texts
SET value = ':enable <id>, :effect <id>, :eff <id> oppure :effect list'
WHERE `key` = 'commands.description.cmd_enable';

UPDATE emulator_texts
SET value = ':handitem <id> oppure :handitem list'
WHERE `key` = 'commands.description.cmd_hand_item';
