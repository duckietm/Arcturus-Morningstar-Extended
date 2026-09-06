-- Auto stack height is opt-in per room: a room with no row is OFF. The column
-- default still said 1, so a hand-written INSERT would have re-enabled it.
ALTER TABLE `room_auto_stack` MODIFY `enabled` TINYINT(1) NOT NULL DEFAULT 0;
