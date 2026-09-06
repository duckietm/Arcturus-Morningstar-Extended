-- Room ownership is capability-based throughout Polaris. Staff ranks at or
-- above Support inherit owner access everywhere, except ranks whose name is a
-- moderator role. Moderator and Super Mod therefore retain ordinary room
-- boundaries, while Support and Administrator can use owner-only commands in
-- any room.
UPDATE `permissions`
SET `acc_anyroomowner` = '1'
WHERE `level` >= 4
  AND LOWER(`rank_name`) NOT LIKE '%mod%';

UPDATE `permissions`
SET `acc_anyroomowner` = '0'
WHERE `level` >= 4
  AND LOWER(`rank_name`) LIKE '%mod%';
