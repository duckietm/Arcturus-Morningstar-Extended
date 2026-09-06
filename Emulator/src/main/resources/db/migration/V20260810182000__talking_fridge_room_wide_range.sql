-- Talking furniture normally reacts only within furniture.talking.range tiles.
-- The Talking Fridge is intended to answer room chat, so give only this furni
-- a room-wide range without changing the behaviour of parrots or other items.
INSERT INTO `emulator_settings` (`key`, `value`, `comment`) VALUES
('talking_fridge.message.range', '100', 'Raggio dedicato del Frigo Parlante; 100 copre l intera stanza.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`), `comment`=VALUES(`comment`);
