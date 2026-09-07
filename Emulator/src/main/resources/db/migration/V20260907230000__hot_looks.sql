-- Hot looks (AIR 13 avatar editor "hotlooks" tab, GetHotLooks 9360 / HotLooks 9360).
--
-- The official tab lists up to 20 ready-made outfits per gender; clicking one
-- loads the figure into the editor (no purchase, the player saves it like any
-- other look). Rows are read straight from this table, so an admin edits the
-- catalogue here: the emulator re-reads it after `hotlooks.cache.seconds`
-- (default 300) without a restart. Empty by default = the tab shows no looks.
--   gender     'M' or 'F' (the client asks for the looks of its current gender)
--   figure     full figure string (hr-...,hd-...); an empty figure is ignored
--   sort_order lower first
--   enabled    0 hides the row without deleting it
CREATE TABLE IF NOT EXISTS `hot_looks` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `gender` CHAR(1) NOT NULL DEFAULT 'M',
    `figure` VARCHAR(255) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `gender_enabled` (`gender`, `enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
