CREATE TABLE IF NOT EXISTS `room_wired_settings` (
  `room_id` int(11) NOT NULL,
  `inspect_mask` int(11) NOT NULL DEFAULT 0 COMMENT 'Bitmask for who can open and inspect Wired in the room. 1=everyone, 2=users with rights, 4=group members, 8=group admins.',
  `modify_mask` int(11) NOT NULL DEFAULT 0 COMMENT 'Bitmask for who can modify Wired in the room. 2=users with rights, 4=group members, 8=group admins.',
  PRIMARY KEY (`room_id`),
  CONSTRAINT `fk_room_wired_settings_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
