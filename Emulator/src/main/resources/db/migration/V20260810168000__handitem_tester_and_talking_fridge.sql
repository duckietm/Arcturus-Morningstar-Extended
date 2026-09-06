UPDATE `items_base` SET `interaction_type`='handitem_tester' WHERE `id`=5406;
UPDATE `items_base` SET `interaction_type`='talking_furni' WHERE `id`=5371;

INSERT INTO `emulator_settings` (`key`, `value`, `comment`) VALUES
('talking_fridge.message.count', '5', 'Numero massimo dei messaggi casuali del Frigo Parlante.'),
('talking_fridge.message.bubble', '28', 'Fumetto usato dal Frigo Parlante.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`), `comment`=VALUES(`comment`);

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
('talking_fridge.message.0', 'Brrr... chi ha lasciato la porta aperta?'),
('talking_fridge.message.1', 'Ho qualcosa di fresco per te!'),
('talking_fridge.message.2', 'Non mangiare tutto in una volta.'),
('talking_fridge.message.3', 'Fa freddino qui dentro.'),
('talking_fridge.message.4', 'La luce si spegne davvero quando chiudi la porta?'),
('talking_fridge.message.5', 'Torna più tardi, sto raffreddando le bibite!')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
