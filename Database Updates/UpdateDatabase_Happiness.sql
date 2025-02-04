UPDATE `emulator_texts` SET `key` = 'generic.pet.happiness', `value` = 'Happiness' WHERE `key` = 'generic.pet.happyness';

ALTER TABLE `pet_commands_data` CHANGE `cost_happyness` `cost_happiness` int(11) NOT NULL DEFAULT '0';
ALTER TABLE `users_pets` CHANGE `happyness` `happiness` int(11) NOT NULL DEFAULT '100';
