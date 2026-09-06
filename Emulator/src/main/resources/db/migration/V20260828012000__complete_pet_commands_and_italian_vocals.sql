-- Every purchasable pet type must expose the same complete training command
-- set.  The normal level gate remains in PetTrainingPanelComposer.
INSERT INTO `pet_actions` (`pet_type`, `pet_name`, `offspring_type`, `happy_actions`, `tired_actions`, `random_actions`, `can_swim`)
SELECT sequence.pet_type, CONCAT('Cucciolo ', sequence.pet_type), -1, '', '', '', '0'
FROM (
    SELECT ones.n + tens.n * 10 AS pet_type
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8) tens
    WHERE ones.n + tens.n * 10 <= 80
) sequence
WHERE NOT EXISTS (SELECT 1 FROM `pet_actions` existing WHERE existing.`pet_type` = sequence.pet_type);

INSERT INTO `pet_commands` (`pet_id`, `command_id`)
SELECT pet_types.pet_id, commands.command_id
FROM (
    SELECT ones.n + tens.n * 10 AS pet_id
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8) tens
    WHERE ones.n + tens.n * 10 <= 80
) pet_types
CROSS JOIN `pet_commands_data` commands
WHERE NOT EXISTS (
    SELECT 1 FROM `pet_commands` existing
    WHERE existing.`pet_id` = pet_types.pet_id AND existing.`command_id` = commands.command_id
);

DELETE FROM `pet_vocals` WHERE `pet_id` = 0;
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(0, 'DISOBEY', 'Mmh, non mi va.'),
(0, 'DISOBEY', 'Non adesso!'),
(0, 'DISOBEY', 'Forse dopo.'),
(0, 'DRINKING', 'Slurp slurp!'),
(0, 'EATING', 'Gnam gnam!'),
(0, 'GENERIC_HAPPY', 'Sìì, giochiamo!'),
(0, 'GENERIC_HAPPY', '*Fischietta felice*'),
(0, 'GENERIC_NEUTRAL', 'Cosa c\'è?'),
(0, 'GENERIC_SAD', 'Sono un po\' triste...'),
(0, 'GREET_OWNER', 'Ciao padrone!'),
(0, 'HUNGRY', 'Ho fame!'),
(0, 'LEVEL_UP', 'Sono diventato più bravo!'),
(0, 'PLAYFUL', 'Giochiamo insieme!'),
(0, 'SLEEPING', 'Zzz...'),
(0, 'THIRSTY', 'Ho sete!'),
(0, 'TIRED', 'Sono stanco...'),
(0, 'UNKNOWN_COMMAND', 'Non capisco questo comando.');
