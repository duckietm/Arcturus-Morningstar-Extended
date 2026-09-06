-- Dice interaction assets use state 0 for closed and states 1 through 6 for
-- their faces. Imported values such as 101 make Polaris select states that do
-- not exist in Nitro, which appears as a die closing after it rolls.
UPDATE items_base
SET interaction_modes_count = 6
WHERE interaction_type = 'dice'
  AND interaction_modes_count <> 6;
