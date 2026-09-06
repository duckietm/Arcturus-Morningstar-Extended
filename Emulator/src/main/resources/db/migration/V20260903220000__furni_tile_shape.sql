-- Per-tile footprint for furni whose artwork is not a plain width x length rectangle.
--
-- An L-shaped sofa draws over eight tiles but could only ever be registered as a rectangle, so half of it
-- was scenery: you walked through it and could not sit on it. `tile_shape` carves tiles out of the
-- rectangle and `sit_directions` gives each remaining tile its own seating direction, because the two arms
-- of an L face different ways.
--
-- Both are authored at rotation 0 and turned with the furni. One character per tile, rows separated by '/':
--   tile_shape      '1111/1000'  -- a 4x2 L: the whole back row, then only the left tile of the front row
--   sit_directions  '4444/2...'  -- back row faces 4, front-left faces 2, '.' inherits the furni rotation
--
-- Empty means "plain rectangle, no per-tile directions", which is how every existing row starts, so nothing
-- changes for furni that have not been through the aligner.
ALTER TABLE `items_base`
    ADD COLUMN `tile_shape` VARCHAR(160) NOT NULL DEFAULT '' AFTER `length`,
    ADD COLUMN `sit_directions` VARCHAR(160) NOT NULL DEFAULT '' AFTER `tile_shape`;
