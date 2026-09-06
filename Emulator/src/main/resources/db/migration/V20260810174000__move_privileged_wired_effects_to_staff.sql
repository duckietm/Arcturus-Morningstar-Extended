-- Privileged effects can mutate accounts, identity, rooms, variables, rewards
-- or navigation. Keep every known imported/public duplicate in Staff Wired.
UPDATE `catalog_items`
SET `page_id`=255,
    `order_number`=10000 + MOD(`id`, 900)
WHERE `id` IN (
    2908,       -- Give Reward (already Staff; kept idempotent)
    2000029902, -- Change Variable Value
    2000029903, -- Make User Say
    2000029906, -- Forward User to Room
    2000029910, -- Give Achievement
    2000029911, -- Give Badge
    2000029914, -- Give Duckets
    2000029915, -- Give Reward
    2000029923, -- Remove Badge
    2000029924, -- Remove Tag
    2000029944, -- Close Room / Close All Gates
    2000029958, -- Give Achievement
    2000029959, -- Give Badge
    2000029964, -- Give Duckets
    2000029982, -- Make User Say
    2000030000, -- Open Room / Open All Gates
    2000030003, -- Remove Badge
    2000030004, -- Remove Tag (imported as Leave Team)
    2000030005, -- Remove Tag
    2000030007, -- Send Alert
    2000030009, -- Send Bubble Alert
    2000030020, -- Teleport Entire Room
    2000030023, -- Teleport to Room
    2000030034, -- Write to Logs
    2000030035, -- Negative Effect: Execute Stacks
    2000030036, -- Negative Effect: Show Message
    2000030037, -- Negative Effect: Write to Logs
    2000269313, -- Close Room duplicate
    2000269314, -- Open Room duplicate
    2000288727, -- Make User Say duplicate
    2137003551, -- Give User Badge imported viewer copy
    2137003886, -- Give User Badge imported viewer copy
    2137006245, -- Alert imported viewer copy
    2137006628  -- Give User Badge imported viewer copy
);
