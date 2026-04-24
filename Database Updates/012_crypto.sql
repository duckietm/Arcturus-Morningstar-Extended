INSERT INTO `emulator_settings` (`key`, `value`) VALUES
  ('crypto.ws.enabled',             '0'),
  ('crypto.ws.signing.enabled',     '0'),
  ('crypto.ws.signing.public_key',  ''),
  ('crypto.ws.signing.private_key', '')
ON DUPLICATE KEY UPDATE `value` = `value`;


