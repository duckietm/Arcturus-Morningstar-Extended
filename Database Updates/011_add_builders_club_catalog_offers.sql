ALTER TABLE `catalog_club_offers`
    MODIFY COLUMN `type` ENUM('HC', 'VIP', 'BUILDERS_CLUB', 'BUILDERS_CLUB_ADDON') NOT NULL DEFAULT 'HC';

ALTER TABLE `users_settings`
    ADD COLUMN `builders_club_bonus_furni` INT NOT NULL DEFAULT 0 AFTER `hc_gifts_claimed`;
