-- Furni Editor imports Italian Habbo names/descriptions by default. The
-- client keeps its existing FurnitureData_%locale%.json override mechanism
-- for users who choose English or another installed locale.
INSERT INTO `emulator_settings` (`key`,`value`) VALUES
    ('furni.editor.import.url','https://www.habbo.it/gamedata/furnidata_json/1')
ON DUPLICATE KEY UPDATE `value`=VALUES(`value`);
