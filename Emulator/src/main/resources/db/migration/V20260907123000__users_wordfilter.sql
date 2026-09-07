-- Personal word filter (AIR 13 "Word filter" row of the ME menu settings).
--
-- Each user keeps a list of words that are masked with the hotel word-filter
-- replacement in the room chat *they* receive. The official client only
-- manages the list (GetCustomFilter 145, AddCustomFilterWord 68,
-- RemoveCustomFilterWord 1996, CustomFilterResult 3883,
-- ModifyCustomFilterResult 3333); the masking itself is server-side, so the
-- list lives next to users_ignored.
CREATE TABLE IF NOT EXISTS `users_wordfilter` (
    `user_id` INT NOT NULL,
    `word` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`user_id`, `word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
