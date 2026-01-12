-- =====================================================
-- Pet Breeding Complete Setup
-- =====================================================
-- This file sets up all breeding-related data:
-- 1. pet_breeding - Maps parent pet types to offspring types
-- 2. pet_breeding_races - Defines possible breeds/colors for offspring by rarity
-- =====================================================

-- =====================================================
-- SECTION 1: Pet Breeding (Parent -> Offspring Mapping)
-- =====================================================
-- This table maps which pet type produces which baby type

CREATE TABLE IF NOT EXISTS `pet_breeding` (
  `pet_id` int(11) NOT NULL COMMENT 'Parent pet type',
  `offspring_id` int(11) NOT NULL COMMENT 'Baby pet type',
  PRIMARY KEY (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Clear existing data
TRUNCATE TABLE `pet_breeding`;

-- Insert breeding mappings
INSERT INTO `pet_breeding` (`pet_id`, `offspring_id`) VALUES
(0, 29),   -- Dog -> Baby Dog
(1, 28),   -- Cat -> Baby Cat
(3, 25),   -- Terrier -> Baby Terrier
(4, 24),   -- Bear -> Baby Bear
(5, 30);   -- Pig -> Baby Pig

-- =====================================================
-- SECTION 2: Pet Breeding Races (Offspring Breeds by Rarity)
-- =====================================================
-- rarity_level: 1=Common, 2=Uncommon, 3=Rare, 4=Epic
-- breed: The visual breed/color variant of the baby pet
-- 
-- Higher rarity = harder to get, more special colors
-- Each baby pet type should have breeds at all 4 rarity levels

CREATE TABLE IF NOT EXISTS `pet_breeding_races` (
  `pet_id` int(11) NOT NULL COMMENT 'Baby pet type (offspring)',
  `rarity_level` int(11) NOT NULL COMMENT '1=Common, 2=Uncommon, 3=Rare, 4=Epic',
  `breed` int(11) NOT NULL COMMENT 'Visual breed/color variant',
  PRIMARY KEY (`pet_id`, `rarity_level`, `breed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Clear existing data
TRUNCATE TABLE `pet_breeding_races`;

-- =====================================================
-- Baby Dog (29) - Offspring of Dog (0) - 20 breeds
-- =====================================================
INSERT INTO `pet_breeding_races` (`pet_type`, `rarity_level`, `breed`) VALUES
-- Common breeds (rarity 1) - Most likely to get
(29, 1, 0),
(29, 1, 1),
(29, 1, 2),
(29, 1, 3),
(29, 1, 4),
(29, 1, 5),
(29, 1, 6),
(29, 1, 7),
-- Uncommon breeds (rarity 2)
(29, 2, 8),
(29, 2, 9),
(29, 2, 10),
(29, 2, 11),
(29, 2, 12),
-- Rare breeds (rarity 3)
(29, 3, 13),
(29, 3, 14),
(29, 3, 15),
(29, 3, 16),
-- Epic breeds (rarity 4) - Hardest to get
(29, 4, 17),
(29, 4, 18),
(29, 4, 19);

-- =====================================================
-- Baby Cat (28) - Offspring of Cat (1) - 20 breeds
-- =====================================================
INSERT INTO `pet_breeding_races` (`pet_type`, `rarity_level`, `breed`) VALUES
-- Common breeds (rarity 1)
(28, 1, 0),
(28, 1, 1),
(28, 1, 2),
(28, 1, 3),
(28, 1, 4),
(28, 1, 5),
(28, 1, 6),
(28, 1, 7),
-- Uncommon breeds (rarity 2)
(28, 2, 8),
(28, 2, 9),
(28, 2, 10),
(28, 2, 11),
(28, 2, 12),
-- Rare breeds (rarity 3)
(28, 3, 13),
(28, 3, 14),
(28, 3, 15),
(28, 3, 16),
-- Epic breeds (rarity 4)
(28, 4, 17),
(28, 4, 18),
(28, 4, 19);

-- =====================================================
-- Baby Terrier (25) - Offspring of Terrier (3) - 20 breeds
-- =====================================================
INSERT INTO `pet_breeding_races` (`pet_type`, `rarity_level`, `breed`) VALUES
-- Common breeds (rarity 1)
(25, 1, 0),
(25, 1, 1),
(25, 1, 2),
(25, 1, 3),
(25, 1, 4),
(25, 1, 5),
(25, 1, 6),
(25, 1, 7),
-- Uncommon breeds (rarity 2)
(25, 2, 8),
(25, 2, 9),
(25, 2, 10),
(25, 2, 11),
(25, 2, 12),
-- Rare breeds (rarity 3)
(25, 3, 13),
(25, 3, 14),
(25, 3, 15),
(25, 3, 16),
-- Epic breeds (rarity 4)
(25, 4, 17),
(25, 4, 18),
(25, 4, 19);

-- =====================================================
-- Baby Bear (24) - Offspring of Bear (4) - 20 breeds
-- =====================================================
INSERT INTO `pet_breeding_races` (`pet_type`, `rarity_level`, `breed`) VALUES
-- Common breeds (rarity 1)
(24, 1, 0),
(24, 1, 1),
(24, 1, 2),
(24, 1, 3),
(24, 1, 4),
(24, 1, 5),
(24, 1, 6),
(24, 1, 7),
-- Uncommon breeds (rarity 2)
(24, 2, 8),
(24, 2, 9),
(24, 2, 10),
(24, 2, 11),
(24, 2, 12),
-- Rare breeds (rarity 3)
(24, 3, 13),
(24, 3, 14),
(24, 3, 15),
(24, 3, 16),
-- Epic breeds (rarity 4)
(24, 4, 17),
(24, 4, 18),
(24, 4, 19);

-- =====================================================
-- Baby Pig (30) - Offspring of Pig (5) - 20 breeds
-- =====================================================
INSERT INTO `pet_breeding_races` (`pet_type`, `rarity_level`, `breed`) VALUES
-- Common breeds (rarity 1)
(30, 1, 0),
(30, 1, 1),
(30, 1, 2),
(30, 1, 3),
(30, 1, 4),
(30, 1, 5),
(30, 1, 6),
(30, 1, 7),
-- Uncommon breeds (rarity 2)
(30, 2, 8),
(30, 2, 9),
(30, 2, 10),
(30, 2, 11),
(30, 2, 12),
-- Rare breeds (rarity 3)
(30, 3, 13),
(30, 3, 14),
(30, 3, 15),
(30, 3, 16),
-- Epic breeds (rarity 4)
(30, 4, 17),
(30, 4, 18),
(30, 4, 19);

-- =====================================================
-- Also ensure pet_actions has correct offspring_type values
-- =====================================================
UPDATE `pet_actions` SET `offspring_type` = 29 WHERE `pet_type` = 0;  -- Dog -> Baby Dog
UPDATE `pet_actions` SET `offspring_type` = 28 WHERE `pet_type` = 1;  -- Cat -> Baby Cat
UPDATE `pet_actions` SET `offspring_type` = 25 WHERE `pet_type` = 3;  -- Terrier -> Baby Terrier
UPDATE `pet_actions` SET `offspring_type` = 24 WHERE `pet_type` = 4;  -- Bear -> Baby Bear
UPDATE `pet_actions` SET `offspring_type` = 30 WHERE `pet_type` = 5;  -- Pig -> Baby Pig

-- Set non-breedable pets to -1
UPDATE `pet_actions` SET `offspring_type` = -1 WHERE `pet_type` NOT IN (0, 1, 3, 4, 5);

-- =====================================================
-- Fix any items_base with leading/trailing spaces in interaction_type
-- =====================================================
UPDATE `items_base` SET `interaction_type` = TRIM(`interaction_type`);

-- =====================================================
-- Ensure breeding nest items have correct interaction_type
-- =====================================================
UPDATE `items_base` SET `interaction_type` = 'breeding_nest' 
WHERE `item_name` LIKE 'pet_breeding_%' AND `interaction_type` != 'breeding_nest';
