ALTER TABLE `camwijs`.`permissions` 
ADD COLUMN `cms_dance` ENUM('0', '1') NULL DEFAULT '0' AFTER `cmd_credits`;

INSERT INTO emulator_texts (`key`, `value`) VALUES ('commands.description.cmd_dance', 'dance around the world ! use 1 t/m 4 and 0 to stop');
INSERT INTO emulator_texts (`key`, `value`) VALUES ('commands.keys.cmd_dance', 'dance');
