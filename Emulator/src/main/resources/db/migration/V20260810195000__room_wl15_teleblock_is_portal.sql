-- Some imported BSS furnidata sets contain a synthetic duplicate of this
-- class with the generic interaction and a non-walkable tile. Every local
-- definition of the class is a Games Portal and must use the portal runtime.
UPDATE `items_base`
SET `interaction_type` = 'teleporttile',
    `allow_walk` = '1'
WHERE `item_name` = 'room_wl15_teleblock';
