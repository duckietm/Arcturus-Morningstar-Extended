-- Recolourable furni catalog page: the buyer picks colour 1 / colour 2 before purchasing and the chosen colours are
-- stored on the bought item (items.wired_data), so the furni keeps them in the room and after a restart.
-- Adds the `recolorable` value to the catalog_pages layout enum; every existing value is kept in its current order.
ALTER TABLE `catalog_pages`
    MODIFY COLUMN `page_layout` enum(
        'default_3x3','club_buy','club_gift','frontpage','spaces','recycler','recycler_info','recycler_prizes',
        'trophies','plasto','marketplace','marketplace_own_items','spaces_new','soundmachine','guilds','guild_furni',
        'info_duckets','info_rentables','info_pets','roomads','single_bundle','sold_ltd_items','badge_display','bots',
        'pets','pets2','pets3','productpage1','room_bundle','recent_purchases','default_3x3_color_grouping',
        'guild_forum','vip_buy','info_loyalty','loyalty_vip_buy','collectibles','petcustomization','frontpage_featured',
        'builders_club_frontpage','builders_club_addons','builders_club_loyalty','root','monkey','niko','mad_money',
        'recolorable'
    ) NOT NULL DEFAULT 'default_3x3';
