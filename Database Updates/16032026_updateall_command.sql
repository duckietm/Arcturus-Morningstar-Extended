ALTER TABLE `permissions` ADD `cmd_update_all` ENUM('0','1') NOT NULL DEFAULT '0' AFTER `cmd_update_achievements`;

INSERT INTO `emulator_texts` (`key`, `value`) VALUES 
  ('commands.keys.cmd_update_all', 'update_all'),
  ('commands.description.cmd_update_all', ':update_all'),
  ('commands.succes.cmd_update_all', 'Successfully updated everything!');
