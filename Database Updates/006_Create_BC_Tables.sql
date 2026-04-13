CREATE TABLE `catalog_items_bc` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_ids` varchar(666) NOT NULL,
  `page_id` int(11) NOT NULL,
  `catalog_name` varchar(100) NOT NULL,
  `order_number` int(11) NOT NULL DEFAULT 1,
  `extradata` varchar(500) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE `catalog_pages_bc` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_id` int(11) NOT NULL DEFAULT -1,
  `caption` varchar(128) NOT NULL,
  `page_layout` enum('default_3x3','club_buy','club_gift','frontpage','spaces','recycler','recycler_info','recycler_prizes','trophies','plasto','marketplace','marketplace_own_items','spaces_new','soundmachine','guilds','guild_furni','info_duckets','info_rentables','info_pets','roomads','single_bundle','sold_ltd_items','badge_display','bots','pets','pets2','pets3','productpage1','room_bundle','recent_purchases','default_3x3_color_grouping','guild_forum','vip_buy','info_loyalty','loyalty_vip_buy','collectibles','petcustomization','frontpage_featured') NOT NULL DEFAULT 'default_3x3',
  `icon_color` int(11) NOT NULL DEFAULT 1,
  `icon_image` int(11) NOT NULL DEFAULT 1,
  `order_num` int(11) NOT NULL DEFAULT 1,
  `visible` enum('0','1') NOT NULL DEFAULT '1',
  `enabled` enum('0','1') NOT NULL DEFAULT '1',
  `page_headline` varchar(1024) NOT NULL DEFAULT '',
  `page_teaser` varchar(64) NOT NULL DEFAULT '',
  `page_special` varchar(2048) DEFAULT '' COMMENT 'Gold Bubble: catalog_special_txtbg1 // Speech Bubble: catalog_special_txtbg2 // Place normal text in page_text_teaser',
  `page_text1` text DEFAULT NULL,
  `page_text2` text DEFAULT NULL,
  `page_text_details` text DEFAULT NULL,
  `page_text_teaser` text DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=MyISAM AUTO_INCREMENT=9 DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci ROW_FORMAT=DYNAMIC;