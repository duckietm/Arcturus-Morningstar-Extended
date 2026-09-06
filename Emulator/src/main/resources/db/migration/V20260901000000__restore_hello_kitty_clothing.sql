-- Restore the purchasable Hello Kitty outfit represented by cookie_hellokittysuit.
-- Figure set 50119 groups head layers 50119-50120; set 50121 groups suit layers 50121-50123.
INSERT INTO `catalog_clothing` (`id`, `name`, `setid`)
VALUES (845, 'cookie_hellokittysuit', '50119,50121'),
       (846, 'clothing_nftpiranha', '6145')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `setid` = VALUES(`setid`);

UPDATE `items_base`
SET `customparams` = '50119,50121'
WHERE `id` = 10407615 AND `item_name` = 'cookie_hellokittysuit';

UPDATE `items_base`
SET `customparams` = '6145'
WHERE `id` = 16653093 AND `item_name` = 'clothing_nftpiranha';
