-- Floor-tile portals activate when a user walks onto them. Hand-imported
-- furnidata rows sometimes retain the teleporttile interaction while losing
-- the walkable flag, which makes the portal and its tile inert.
UPDATE `items_base`
SET `allow_walk` = '1'
WHERE `interaction_type` IN ('teleporttile', 'club_teleporttile');
