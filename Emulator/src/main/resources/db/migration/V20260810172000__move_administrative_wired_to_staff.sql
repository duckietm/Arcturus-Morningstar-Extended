-- Currency, identity and command-execution Wired effects are administrative
-- tools. Keep every public duplicate in the existing Staff Wired page.
UPDATE `catalog_pages`
SET `parent_id`=7,
    `caption_save`='staff_wired',
    `caption`='Staff Wired',
    `min_rank`=6,
    `visible`='1',
    `enabled`='1'
WHERE `id`=255;

UPDATE `catalog_items`
SET `page_id`=255,
    `order_number`=9100 + MOD(`id`, 800)
WHERE `id` IN (
    2000029901, -- Give Tag
    2000029912, -- Give Credits
    2000029913, -- Give Diamonds
    2000029961, -- Give Credits
    2000029963, -- Give Diamonds
    2000029968, -- Give Perm Tag
    2000029970, -- Give Prefix
    2000029972, -- Give Tag
    2000029973, -- Give Tag
    2000029974, -- Give XP
    2000029980, -- Make User Execute Command
    2137003172  -- BSS Give Diamonds
);
