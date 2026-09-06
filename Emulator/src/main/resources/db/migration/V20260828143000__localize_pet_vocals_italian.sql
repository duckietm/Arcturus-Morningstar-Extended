DELETE FROM pet_vocals;

INSERT INTO pet_vocals (pet_id, type, message) VALUES
(0, 'DISOBEY', 'Non ne ho voglia adesso.'),
(0, 'DISOBEY', 'Forse tra poco!'),
(0, 'DISOBEY', 'Non ho capito, riprova.'),
(0, 'GENERIC_HAPPY', 'Evviva, giochiamo!'),
(0, 'GENERIC_HAPPY', '*Fischietta felice*'),
(0, 'GENERIC_NEUTRAL', '*Ti guarda curioso*'),
(0, 'GENERIC_SAD', '*Sembra un po'' triste*'),
(0, 'GREET_OWNER', 'Ciao! Che bello rivederti!'),
(0, 'HUNGRY', 'Ho fame!'),
(0, 'LEVEL_UP', 'Sono diventato più bravo!'),
(0, 'MUTED', '*Resta in silenzio*'),
(0, 'PLAYFUL', 'Giochiamo insieme!'),
(0, 'SLEEPING', 'Zzz...'),
(0, 'THIRSTY', 'Ho sete!'),
(0, 'TIRED', 'Sono stanco, fammi riposare.'),
(0, 'UNKNOWN_COMMAND', 'Non conosco ancora questo comando.');

-- Every registered pet type exposes every implemented command. PetCommand
-- still executes the matching action; ownership policy controls obedience.
DELETE FROM pet_commands;
INSERT INTO pet_commands (pet_id, command_id)
SELECT actions.pet_type, commands.command_id
FROM pet_actions actions
CROSS JOIN pet_commands_data commands;
