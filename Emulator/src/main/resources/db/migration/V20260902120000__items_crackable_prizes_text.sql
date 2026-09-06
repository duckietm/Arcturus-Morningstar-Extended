-- Furni editor crackable configuration: the prize list ("itemId:chance;...") was capped at 255 characters,
-- which is about 25 prizes. Widen it so the in-game editor can store long prize tables.
--
-- `prizes` is also part of the composite `data` index. TEXT columns cannot be indexed at full
-- length here: MyISAM caps a composite key at 1000 bytes total, and the previous varchar(255)
-- column was already indexed in full, so a naive MODIFY blows that limit ("Specified key was too
-- long; max key length is 1000 bytes"). Rebuild the index with a 255-byte prefix on `prizes` -
-- identical to its previous indexed length - so index behaviour is unchanged while the storage
-- cap is lifted.
ALTER TABLE `items_crackable`
	DROP KEY `data`,
	MODIFY `prizes` TEXT NOT NULL DEFAULT '179:1' COMMENT 'Used in the format of item_id:chance;item_id_2:chance. item_id must be id in the items_base table. Default value for chance is 100.',
	ADD KEY `data` (`count`,`prizes`(255),`achievement_tick`,`achievement_cracked`) USING BTREE;
