-- Talking furniture reads count/bubble from emulator_settings, but messages
-- from emulator_texts. Reassert both locations and clear persisted active state
-- left behind by older restart-sensitive cooldown behaviour.
UPDATE `items_base`
SET `interaction_type`='talking_furni', `interaction_modes_count`=2
WHERE `id`=5371 OR LOWER(`item_name`)='talking_fridge';

INSERT INTO `emulator_settings` (`key`, `value`, `comment`) VALUES
('talking_fridge.message.count', '5', 'Indice massimo dei messaggi casuali del Frigo Parlante.'),
('talking_fridge.message.bubble', '28', 'Fumetto usato dal Frigo Parlante.')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`), `comment`=VALUES(`comment`);

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
('talking_fridge.message.0', 'Brrr... chi ha lasciato la porta aperta?'),
('talking_fridge.message.1', 'Ho qualcosa di fresco per te!'),
('talking_fridge.message.2', 'Non mangiare tutto in una volta.'),
('talking_fridge.message.3', 'Fa freddino qui dentro.'),
('talking_fridge.message.4', 'La luce si spegne davvero quando chiudi la porta?'),
('talking_fridge.message.5', 'Torna piu tardi, sto raffreddando le bibite!')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);

UPDATE `items`
SET `extra_data`='0'
WHERE `item_id` IN (
    SELECT `id` FROM `items_base` WHERE LOWER(`item_name`)='talking_fridge'
);
