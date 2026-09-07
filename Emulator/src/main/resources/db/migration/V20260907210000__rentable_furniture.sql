-- Rentable furniture (AIR 13 rent offers, infostand "Extend" / "Buy-out").
--
-- A catalog offer with rent_days > 0 is a rental: the client shows it with
-- the "Rent" caption (offer flag isRent) and the purchased furni carries an
-- expiry. items.expires is the unix timestamp after which the emulator
-- removes the furni (-1 = owned outright); it is serialised to the client
-- as the seconds left on floor / wall / inventory items, which drives the
-- infostand "Rental time remaining" text and the extend / buy-out buttons
-- (GetRentOrBuyoutOffer 2518, ExtendRentOrBuyoutFurni 1071,
-- ExtendRentOrBuyoutStripItem 2115, FurniRentOrBuyoutOffer 35).
ALTER TABLE `items` ADD COLUMN IF NOT EXISTS `expires` INT NOT NULL DEFAULT -1;
ALTER TABLE `catalog_items` ADD COLUMN IF NOT EXISTS `rent_days` INT NOT NULL DEFAULT 0;
