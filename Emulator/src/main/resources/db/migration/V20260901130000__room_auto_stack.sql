CREATE TABLE IF NOT EXISTS `room_auto_stack` (
  `room_id` INT NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
  ('commands.keys.cmd_autostackheight', 'autostackheight;autostack;altezzaauto'),
  ('commands.description.cmd_autostackheight', ':autostackheight [on|off] - regola automaticamente l''altezza dei Furni spostati o ruotati (proprietari stanza)')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
