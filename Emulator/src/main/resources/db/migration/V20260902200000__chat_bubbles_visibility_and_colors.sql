-- Chat bubbles managed from the housekeeping: hide a bubble from ranks above max_rank (0 = no limit)
-- and draw bubbles that have no client PNG with flat colours (empty = the client uses its own art).
ALTER TABLE `chat_bubbles`
    ADD COLUMN IF NOT EXISTS `max_rank` INT(11) NOT NULL DEFAULT 0 AFTER `min_rank`,
    ADD COLUMN IF NOT EXISTS `color` VARCHAR(9) NOT NULL DEFAULT '' AFTER `duration_weeks`,
    ADD COLUMN IF NOT EXISTS `text_color` VARCHAR(9) NOT NULL DEFAULT '' AFTER `color`;
