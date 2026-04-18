INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
  ('furni.editor.renderer.config.path', '/var/www/Gamedata/config/renderer-config.json'),
  ('furni.editor.asset.base.path', '/var/www/Gamedata/furniture/nitro-assets/');
  
ALTER TABLE permissions
ADD COLUMN `acc_catalogfurni` ENUM('0','1') NOT NULL DEFAULT '0' AFTER `acc_catalog_ids`;