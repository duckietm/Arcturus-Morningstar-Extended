-- Nitro SFX furni play their embedded audio when their state is toggled.
-- Keep every classic and custom SFX base on the registered sound_fx handler.
UPDATE items_base
SET interaction_type = 'sound_fx'
WHERE LOWER(item_name) LIKE 'sfx\_%' ESCAPE '\\'
   OR LOWER(item_name) LIKE '%\_sfx%' ESCAPE '\\';

-- Repair the legacy mannequin default containing a literal-space figure.
UPDATE items
SET extra_data = 'm::My look'
WHERE extra_data IN ('m: :My look', 'm: :My Look');
