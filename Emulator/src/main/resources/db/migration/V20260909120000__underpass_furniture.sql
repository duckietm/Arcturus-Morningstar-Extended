-- Per-item walk-underneath flag (build height widget "allow underpass" selector).
--
-- items.allow_underpass marks a single furni as passable underneath when it is
-- raised at least RoomLayout.UNDERPASS_HEIGHT above the walk surface, even when
-- the room-wide rooms.allow_underpass setting is off. It is set by placing or
-- moving furniture while the build-underpass mode is on (SetBuildUnderpass 7022,
-- sent by the client's build height widget) and cleared by re-placing the item
-- from the inventory with the mode off.
ALTER TABLE `items` ADD COLUMN IF NOT EXISTS `allow_underpass` TINYINT NOT NULL DEFAULT 0;
