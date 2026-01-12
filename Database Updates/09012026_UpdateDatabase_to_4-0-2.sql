SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- SECTION 1: Pet System Emulator Settings
-- =====================================================

INSERT INTO `emulator_settings` (`key`, `value`) VALUES
-- Core pet limits
('hotel.pets.max.room', '15'),
('hotel.pets.max.inventory', '25'),
('hotel.pets.name.length.min', '1'),
('hotel.pets.name.length.max', '15'),
('hotel.daily.respect.pets', '3'),

-- Command cooldown and spam prevention
('pet.command.cooldown_ms', '2000'),
('pet.command.max_same_spam', '3'),
('pet.command.spam_reset_ms', '10000'),
('pet.command.min_energy', '15'),
('pet.command.min_happiness', '10'),
('pet.command.base_obey_chance', '70'),

-- Pet behavior settings
('pet.behavior.autonomous_action_delay', '5000'),
('pet.behavior.idle_wander_min_ms', '10000'),
('pet.behavior.idle_wander_max_ms', '30000'),

-- Pet stats decay/recovery rates (per cycle)
('pet.stats.hunger_decay', '1'),
('pet.stats.thirst_decay', '1'),
('pet.stats.energy_decay', '1'),
('pet.stats.happiness_decay', '1'),
('pet.stats.energy_recovery', '5'),
('pet.stats.happiness_recovery', '1'),

-- Pet thresholds (below this = needs attention)
('pet.threshold.hungry', '50'),
('pet.threshold.thirsty', '50'),
('pet.threshold.tired', '30'),
('pet.threshold.sad', '30'),

-- Pet breeding
('pet.breeding.timeout_seconds', '120')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);

-- =====================================================
-- SECTION 2: Pet Actions (Pet Type Definitions)
-- =====================================================

DROP TABLE IF EXISTS `pet_actions`;
CREATE TABLE `pet_actions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pet_type` int(11) NOT NULL,
  `pet_name` varchar(32) NOT NULL DEFAULT '',
  `offspring_type` int(11) NOT NULL DEFAULT -1,
  `happy_actions` varchar(100) NOT NULL DEFAULT 'sml',
  `tired_actions` varchar(100) NOT NULL DEFAULT 'trd',
  `random_actions` varchar(100) NOT NULL DEFAULT 'lov',
  `can_swim` enum('0','1') NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `pet_type` (`pet_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `pet_actions` (`pet_type`, `pet_name`, `offspring_type`, `happy_actions`, `tired_actions`, `random_actions`, `can_swim`) VALUES
(0, 'Dog', 29, 'sml,wav,joy', 'trd,yng', 'lov,snf', '0'),
(1, 'Cat', 28, 'sml,pur', 'trd,yng', 'lov,lck', '0'),
(2, 'Crocodile', -1, 'sml', 'trd', 'lov,snp', '1'),
(3, 'Terrier', 25, 'sml,wav,joy', 'trd,yng', 'lov,snf', '0'),
(4, 'Bear', 24, 'sml,grw', 'trd,yng', 'lov', '0'),
(5, 'Pig', 30, 'sml,oink', 'trd,yng', 'lov,rol', '0'),
(6, 'Lion', -1, 'sml,ror', 'trd,yng', 'lov', '0'),
(7, 'Rhino', -1, 'sml', 'trd,yng', 'lov', '0'),
(8, 'Tarantula', -1, 'sml', 'trd', 'lov,crw', '0'),
(9, 'Turtle', -1, 'sml', 'trd', 'lov', '1'),
(10, 'Chick', -1, 'sml,chp', 'trd', 'lov,pck', '0'),
(11, 'Frog', -1, 'sml,crk', 'trd', 'lov,jmp', '1'),
(12, 'Dragon', -1, 'sml,flm', 'trd,smk', 'lov,fly', '0'),
(13, 'Monster', -1, 'sml', 'trd', 'lov', '0'),
(14, 'Monkey', -1, 'sml,ook', 'trd,yng', 'lov,swg', '0'),
(15, 'Horse', -1, 'sml,nei', 'trd,yng', 'lov', '0'),
(16, 'Monsterplant', -1, 'sml', 'trd', 'lov', '0'),
(17, 'Bunny', -1, 'sml,hop', 'trd,yng', 'lov', '0'),
(18, 'Evil Bunny', -1, 'sml', 'trd', 'lov', '0'),
(19, 'Bored Bunny', -1, 'sml', 'trd', 'lov', '0'),
(20, 'Cute Bunny', -1, 'sml,hop', 'trd', 'lov', '0'),
(21, 'Wise Pigeon', -1, 'sml,coo', 'trd', 'lov,pck', '0'),
(22, 'Evil Pigeon', -1, 'sml', 'trd', 'lov,pck', '0'),
(23, 'Evil Monkey', -1, 'sml', 'trd', 'lov,swg', '0'),
(24, 'Baby Bear', -1, 'sml', 'trd,yng', 'lov', '0'),
(25, 'Baby Terrier', -1, 'sml', 'trd,yng', 'lov', '0'),
(26, 'Gnome', -1, 'sml,grn', 'trd', 'lov', '0'),
(27, 'Leprechaun', -1, 'sml,grn', 'trd', 'lov,jig', '0'),
(28, 'Baby Cat', -1, 'sml', 'trd,yng', 'lov', '0'),
(29, 'Baby Dog', -1, 'sml', 'trd,yng', 'lov', '0'),
(30, 'Baby Pig', -1, 'sml', 'trd,yng', 'lov', '0'),
(31, 'Haloompa', -1, 'sml', 'trd', 'lov', '0'),
(32, 'Fools Pet', -1, 'sml', 'trd', 'lov', '0'),
(33, 'Pterodactyl', -1, 'sml,sqk', 'trd', 'lov,fly', '0'),
(34, 'Velociraptor', -1, 'sml,hss', 'trd', 'lov,clw', '0'),
(35, 'Cow', -1, 'sml,moo', 'trd,yng', 'lov,chw', '0');

-- =====================================================
-- SECTION 3: Pet Commands Data (English Command Names)
-- =====================================================
-- Command IDs mapped to PetManager.petActions:
-- 0=Free, 1=Sit, 2=Down, 3=Here, 4=Beg, 5=PlayDead, 6=Stay, 7=Follow
-- 8=Stand, 9=Jump, 10=Speak, 11=Play, 12=Silent, 13=Nest, 14=Drink
-- 15=FollowLeft, 16=FollowRight, 17=PlayFootball, 18=Teleport, 19=Bounce
-- 20=Flatten, 21=Dance, 22=Spin, 23=Switch, 24=MoveForward
-- 25=TurnLeft, 26=TurnRight, 27=Relax, 28=Croak, 29=Dip, 30=Wave
-- 31=Mambo, 32=HighJump, 33=ChickenDance, 34=TripleJump
-- 35=Wings, 36=BreatheFire, 37=Hang, 38=Torch, 40=Swing, 41=Roll
-- 42=RingOfFire, 43=Eat, 44=WagTail, 45=Count, 46=Breed

DROP TABLE IF EXISTS `pet_commands_data`;
CREATE TABLE `pet_commands_data` (
  `command_id` int(11) NOT NULL,
  `text` varchar(25) NOT NULL,
  `required_level` int(11) NOT NULL DEFAULT 1,
  `reward_xp` int(11) NOT NULL DEFAULT 5,
  `cost_happiness` int(11) NOT NULL DEFAULT 0,
  `cost_energy` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`command_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `pet_commands_data` (`command_id`, `text`, `required_level`, `reward_xp`, `cost_happiness`, `cost_energy`) VALUES
(0, 'free', 1, 5, 0, 0),
(1, 'sit', 1, 5, 2, 2),
(2, 'down', 2, 10, 3, 3),
(3, 'come here', 2, 10, 2, 5),
(4, 'beg', 2, 10, 3, 4),
(5, 'play dead', 3, 15, 4, 5),
(6, 'stay', 4, 10, 2, 3),
(7, 'follow', 5, 15, 3, 8),
(8, 'stand', 6, 15, 2, 3),
(9, 'jump', 6, 15, 4, 8),
(10, 'speak', 7, 10, 3, 3),
(11, 'play', 8, 5, 5, 10),
(12, 'silent', 8, 5, 2, 1),
(13, 'nest', 5, 5, 0, 0),
(14, 'drink', 1, 5, 0, 0),
(15, 'follow left', 15, 15, 4, 10),
(16, 'follow right', 15, 15, 4, 10),
(17, 'play football', 10, 5, 5, 12),
(18, 'teleport', 9, 5, 3, 5),
(19, 'bounce', 9, 5, 5, 10),
(20, 'flatten', 11, 5, 3, 4),
(21, 'dance', 12, 10, 6, 12),
(22, 'spin', 10, 5, 4, 8),
(23, 'switch', 12, 5, 3, 3),
(24, 'move forward', 17, 5, 2, 2),
(25, 'turn left', 18, 5, 2, 2),
(26, 'turn right', 18, 5, 2, 2),
(27, 'relax', 13, 5, 0, 0),
(28, 'croak', 14, 5, 3, 3),
(29, 'dip', 14, 5, 5, 10),
(30, 'wave', 5, 5, 2, 3),
(31, 'mambo', 18, 5, 6, 12),
(32, 'high jump', 18, 5, 5, 12),
(33, 'chicken dance', 7, 5, 5, 10),
(34, 'triple jump', 9, 5, 6, 15),
(35, 'spread wings', 8, 5, 4, 6),
(36, 'breathe fire', 10, 5, 5, 8),
(37, 'hang', 12, 5, 4, 6),
(38, 'torch', 6, 5, 3, 5),
(40, 'swing', 13, 5, 4, 8),
(41, 'roll', 10, 5, 5, 10),
(42, 'ring of fire', 20, 10, 8, 15),
(43, 'eat', 1, 5, 0, 0),
(44, 'wag tail', 4, 5, 3, 4),
(45, 'count', 6, 5, 4, 5),
(46, 'breed', 1, 5, 10, 20);

-- =====================================================
-- SECTION 4: Pet Commands (Pet Type -> Command Mapping)
-- =====================================================

DROP TABLE IF EXISTS `pet_commands`;
CREATE TABLE `pet_commands` (
  `pet_id` int(11) NOT NULL,
  `command_id` int(11) NOT NULL,
  PRIMARY KEY (`pet_id`, `command_id`),
  KEY `pet_id` (`pet_id`),
  KEY `command_id` (`command_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dog (0) - Full standard pet commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6), (0, 7), (0, 8), (0, 9),
(0, 10), (0, 11), (0, 12), (0, 13), (0, 14), (0, 15), (0, 16), (0, 17), (0, 24),
(0, 25), (0, 26), (0, 43), (0, 44), (0, 46);

-- Cat (1) - Full standard pet commands + breed
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(1, 0), (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 24),
(1, 25), (1, 26), (1, 43), (1, 46);

-- Crocodile (2) - Standard commands (can swim)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(2, 0), (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9),
(2, 10), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16), (2, 17), (2, 24),
(2, 25), (2, 26), (2, 29), (2, 43);

-- Terrier (3) - Standard commands + breed
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(3, 0), (3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6), (3, 7), (3, 8), (3, 9),
(3, 10), (3, 11), (3, 12), (3, 13), (3, 14), (3, 15), (3, 16), (3, 17), (3, 24),
(3, 25), (3, 26), (3, 43), (3, 46);

-- Bear (4) - Standard commands + breed
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(4, 0), (4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9),
(4, 10), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15), (4, 16), (4, 17), (4, 24),
(4, 25), (4, 26), (4, 43), (4, 46);

-- Pig (5) - Standard commands + breed
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(5, 0), (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 7), (5, 8), (5, 9),
(5, 10), (5, 11), (5, 12), (5, 13), (5, 14), (5, 15), (5, 16), (5, 17), (5, 24),
(5, 25), (5, 26), (5, 43), (5, 46);

-- Lion (6) - Standard commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(6, 0), (6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (6, 7), (6, 8), (6, 9),
(6, 10), (6, 11), (6, 12), (6, 13), (6, 14), (6, 15), (6, 16), (6, 17), (6, 24),
(6, 25), (6, 26), (6, 43);

-- Rhino (7) - Standard commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(7, 0), (7, 1), (7, 2), (7, 3), (7, 4), (7, 5), (7, 6), (7, 7), (7, 8), (7, 9),
(7, 10), (7, 11), (7, 12), (7, 13), (7, 14), (7, 15), (7, 16), (7, 17), (7, 24),
(7, 25), (7, 26), (7, 43);

-- Tarantula (8) - Spider commands (bounce, flatten, spin, etc.)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(8, 0), (8, 2), (8, 3), (8, 5), (8, 6), (8, 7), (8, 9), (8, 10), (8, 11), (8, 13),
(8, 14), (8, 15), (8, 16), (8, 17), (8, 19), (8, 20), (8, 21), (8, 22), (8, 23),
(8, 24), (8, 25), (8, 26), (8, 43);

-- Turtle (9) - Aquatic commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(9, 0), (9, 1), (9, 2), (9, 3), (9, 6), (9, 7), (9, 8), (9, 10), (9, 11), (9, 13),
(9, 14), (9, 15), (9, 16), (9, 24), (9, 25), (9, 26), (9, 29), (9, 41), (9, 43);

-- Chick (10) - Bird commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(10, 0), (10, 2), (10, 3), (10, 6), (10, 7), (10, 11), (10, 13), (10, 15), (10, 16),
(10, 17), (10, 33);

-- Frog (11) - Amphibian commands (croak, dip, wave, mambo)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(11, 0), (11, 1), (11, 2), (11, 3), (11, 4), (11, 5), (11, 6), (11, 7), (11, 9),
(11, 13), (11, 14), (11, 15), (11, 16), (11, 17), (11, 27), (11, 28), (11, 29),
(11, 30), (11, 31), (11, 43);

-- Dragon (12) - Dragon special commands (fire, hang, swing, ring of fire)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(12, 0), (12, 2), (12, 3), (12, 5), (12, 6), (12, 7), (12, 8), (12, 9), (12, 10),
(12, 11), (12, 12), (12, 13), (12, 14), (12, 15), (12, 16), (12, 22), (12, 35),
(12, 36), (12, 37), (12, 38), (12, 40), (12, 41), (12, 42), (12, 43);

-- Monster (13) - Basic commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(13, 0), (13, 2), (13, 3), (13, 6), (13, 7), (13, 13);

-- Monkey (14) - Monkey commands (wave, hang, swing)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(14, 0), (14, 1), (14, 2), (14, 3), (14, 4), (14, 5), (14, 6), (14, 7), (14, 9),
(14, 13), (14, 14), (14, 15), (14, 16), (14, 17), (14, 27), (14, 29), (14, 30),
(14, 31), (14, 37), (14, 40), (14, 43);

-- Horse (15) - Rideable pet commands + wag tail, count
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(15, 0), (15, 2), (15, 3), (15, 6), (15, 7), (15, 10), (15, 11), (15, 12), (15, 13),
(15, 14), (15, 15), (15, 16), (15, 24), (15, 25), (15, 26), (15, 43), (15, 44), (15, 45);

-- Monsterplant (16) - Minimal commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(16, 0), (16, 14), (16, 43);

-- Bunnies (17-20) - Bunny commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(17, 0), (17, 2), (17, 3), (17, 6), (17, 7), (17, 11), (17, 13), (17, 15), (17, 16), (17, 17),
(18, 0), (18, 2), (18, 3), (18, 6), (18, 7), (18, 11), (18, 13), (18, 15), (18, 16), (18, 17),
(19, 0), (19, 2), (19, 3), (19, 6), (19, 7), (19, 11), (19, 13), (19, 15), (19, 16), (19, 17),
(20, 0), (20, 2), (20, 3), (20, 6), (20, 7), (20, 11), (20, 13), (20, 15), (20, 16), (20, 17);

-- Pigeons (21-22)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(21, 0), (21, 2), (21, 3), (21, 6), (21, 7), (21, 11), (21, 13), (21, 15), (21, 16), (21, 17),
(22, 0), (22, 2), (22, 3), (22, 6), (22, 7), (22, 11), (22, 13), (22, 15), (22, 16), (22, 17);

-- Evil Monkey (23) - Monkey commands
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(23, 0), (23, 1), (23, 2), (23, 3), (23, 4), (23, 5), (23, 6), (23, 7), (23, 9),
(23, 13), (23, 14), (23, 15), (23, 16), (23, 17), (23, 25), (23, 26), (23, 27),
(23, 29), (23, 30), (23, 31), (23, 37), (23, 40), (23, 43);

-- Baby Bear (24)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(24, 0), (24, 1), (24, 2), (24, 3), (24, 4), (24, 6), (24, 7), (24, 8), (24, 10),
(24, 11), (24, 12), (24, 13), (24, 14), (24, 15), (24, 16), (24, 17), (24, 24),
(24, 25), (24, 26), (24, 43);

-- Baby Terrier (25)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(25, 0), (25, 1), (25, 2), (25, 3), (25, 4), (25, 6), (25, 7), (25, 8), (25, 10),
(25, 11), (25, 12), (25, 13), (25, 14), (25, 15), (25, 16), (25, 17), (25, 24),
(25, 25), (25, 26), (25, 43);

-- Gnome (26)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(26, 0), (26, 1), (26, 2), (26, 3), (26, 4), (26, 6), (26, 7), (26, 8), (26, 13),
(26, 14), (26, 15), (26, 16), (26, 17), (26, 25), (26, 26), (26, 43);

-- Leprechaun (27)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(27, 0), (27, 1), (27, 2), (27, 3), (27, 4), (27, 6), (27, 7), (27, 8), (27, 13),
(27, 14), (27, 15), (27, 16), (27, 17), (27, 25), (27, 26), (27, 43);

-- Baby Cat (28)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(28, 0), (28, 1), (28, 2), (28, 3), (28, 4), (28, 6), (28, 7), (28, 8), (28, 10),
(28, 11), (28, 12), (28, 13), (28, 14), (28, 15), (28, 16), (28, 17), (28, 24),
(28, 25), (28, 26), (28, 43);

-- Baby Dog (29)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(29, 0), (29, 1), (29, 2), (29, 3), (29, 4), (29, 6), (29, 7), (29, 8), (29, 10),
(29, 11), (29, 12), (29, 13), (29, 14), (29, 15), (29, 16), (29, 17), (29, 24),
(29, 25), (29, 26), (29, 43);

-- Baby Pig (30)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(30, 0), (30, 1), (30, 2), (30, 3), (30, 4), (30, 6), (30, 7), (30, 8), (30, 10),
(30, 11), (30, 12), (30, 13), (30, 14), (30, 15), (30, 16), (30, 17), (30, 24),
(30, 25), (30, 26), (30, 43);

-- Haloompa (31)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(31, 0), (31, 1), (31, 2), (31, 3), (31, 4), (31, 6), (31, 7), (31, 8), (31, 13),
(31, 14), (31, 15), (31, 16), (31, 17), (31, 25), (31, 26), (31, 43);

-- Fools Pet (32) - Full dance/trick set
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(32, 0), (32, 1), (32, 2), (32, 3), (32, 4), (32, 5), (32, 6), (32, 7), (32, 8),
(32, 9), (32, 13), (32, 14), (32, 15), (32, 16), (32, 17), (32, 21), (32, 25),
(32, 26), (32, 43);

-- Pterodactyl (33)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(33, 0), (33, 2), (33, 3), (33, 4), (33, 6), (33, 7), (33, 11), (33, 13), (33, 14),
(33, 15), (33, 16), (33, 25), (33, 26), (33, 43);

-- Velociraptor (34)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(34, 0), (34, 1), (34, 2), (34, 3), (34, 6), (34, 7), (34, 8), (34, 10), (34, 12),
(34, 13), (34, 14), (34, 15), (34, 16), (34, 17), (34, 21), (34, 26), (34, 43);

-- Cow (35)
INSERT INTO `pet_commands` (`pet_id`, `command_id`) VALUES
(35, 0), (35, 2), (35, 3), (35, 4), (35, 6), (35, 7), (35, 13), (35, 14), (35, 15),
(35, 16), (35, 17), (35, 25), (35, 26), (35, 30), (35, 43);

-- =====================================================
-- SECTION 5: Pet Vocals (Pet Speech Messages)
-- =====================================================
-- pet_id = -1 means general vocals for all pets
-- pet_id >= 0 means specific to that pet type

CREATE TABLE IF NOT EXISTS `pet_vocals` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pet_id` int(11) NOT NULL DEFAULT -1,
  `type` varchar(20) NOT NULL,
  `message` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `pet_id` (`pet_id`),
  KEY `type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Clear existing vocals
DELETE FROM `pet_vocals`;

-- =====================================================
-- GENERAL VOCALS (pet_id = -1, used by all pets)
-- =====================================================

-- GREET_OWNER - When owner enters room
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'GREET_OWNER', '*perks up excitedly*'),
(-1, 'GREET_OWNER', 'You''re back!'),
(-1, 'GREET_OWNER', '*bounces with joy*'),
(-1, 'GREET_OWNER', 'I missed you!'),
(-1, 'GREET_OWNER', '*runs in circles happily*'),
(-1, 'GREET_OWNER', 'Yay! My favorite person!'),
(-1, 'GREET_OWNER', '*jumps up and down*'),
(-1, 'GREET_OWNER', 'Finally! You''re here!'),
(-1, 'GREET_OWNER', '*tail wagging intensifies*');

-- LEVEL_UP - When pet gains a level
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'LEVEL_UP', '*jumps with joy!*'),
(-1, 'LEVEL_UP', 'I leveled up!'),
(-1, 'LEVEL_UP', 'I feel stronger!'),
(-1, 'LEVEL_UP', 'Woohoo! New level!'),
(-1, 'LEVEL_UP', '*celebrates*'),
(-1, 'LEVEL_UP', 'I''m getting better!'),
(-1, 'LEVEL_UP', 'Level up! Yeah!');

-- MUTED - When told to be silent
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'MUTED', '*stays quiet*'),
(-1, 'MUTED', '...'),
(-1, 'MUTED', '*zips lips*'),
(-1, 'MUTED', '*nods silently*');

-- UNKNOWN_COMMAND - When pet doesn't understand
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'UNKNOWN_COMMAND', '*tilts head confused*'),
(-1, 'UNKNOWN_COMMAND', 'Huh?'),
(-1, 'UNKNOWN_COMMAND', 'I don''t understand...'),
(-1, 'UNKNOWN_COMMAND', '*looks puzzled*'),
(-1, 'UNKNOWN_COMMAND', 'What do you mean?'),
(-1, 'UNKNOWN_COMMAND', '*scratches head*');

-- DISOBEY - When pet refuses command
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'DISOBEY', '*ignores command*'),
(-1, 'DISOBEY', 'Maybe later...'),
(-1, 'DISOBEY', 'I don''t feel like it'),
(-1, 'DISOBEY', '*pretends not to hear*'),
(-1, 'DISOBEY', 'Nah...'),
(-1, 'DISOBEY', '*turns away*'),
(-1, 'DISOBEY', 'Not right now'),
(-1, 'DISOBEY', '*yawns dismissively*'),
(-1, 'DISOBEY', 'Too tired for that'),
(-1, 'DISOBEY', 'Ask me again later'),
(-1, 'DISOBEY', '*looks the other way*'),
(-1, 'DISOBEY', 'I''d rather not'),
(-1, 'DISOBEY', '*shakes head*'),
(-1, 'DISOBEY', 'No thanks'),
(-1, 'DISOBEY', '*walks away slowly*'),
(-1, 'DISOBEY', 'Can''t be bothered'),
(-1, 'DISOBEY', '*pretends to be asleep*'),
(-1, 'DISOBEY', 'You can''t make me!'),
(-1, 'DISOBEY', '*stubbornly sits down*'),
(-1, 'DISOBEY', 'I refuse!');

-- DRINKING - When drinking water
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'DRINKING', '*laps up water*'),
(-1, 'DRINKING', 'Refreshing!'),
(-1, 'DRINKING', '*gulp gulp*'),
(-1, 'DRINKING', 'Ah, that''s good!'),
(-1, 'DRINKING', '*slurp*');

-- EATING - When eating food
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'EATING', '*munches happily*'),
(-1, 'EATING', 'Yum!'),
(-1, 'EATING', 'Delicious!'),
(-1, 'EATING', '*nom nom nom*'),
(-1, 'EATING', 'This is tasty!'),
(-1, 'EATING', '*chomps*'),
(-1, 'EATING', 'More please!');

-- PLAYFUL - When in playful mood
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'PLAYFUL', '*bounces excitedly*'),
(-1, 'PLAYFUL', 'Let''s play!'),
(-1, 'PLAYFUL', '*runs around happily*'),
(-1, 'PLAYFUL', 'Play with me!'),
(-1, 'PLAYFUL', '*jumps around*'),
(-1, 'PLAYFUL', 'I wanna play!'),
(-1, 'PLAYFUL', '*brings a toy*'),
(-1, 'PLAYFUL', 'Wheee!'),
(-1, 'PLAYFUL', '*zooms around the room*'),
(-1, 'PLAYFUL', 'Catch me if you can!');

-- SLEEPING - When sleeping
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'SLEEPING', '*snores softly*'),
(-1, 'SLEEPING', 'Zzz...'),
(-1, 'SLEEPING', '*mumbles in sleep*'),
(-1, 'SLEEPING', 'ZzZzZz...'),
(-1, 'SLEEPING', '*dreams peacefully*'),
(-1, 'SLEEPING', '*twitches while dreaming*'),
(-1, 'SLEEPING', '*snoozes*'),
(-1, 'SLEEPING', '*breathes slowly*');

-- TIRED - When tired/low energy
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'TIRED', '*yawns*'),
(-1, 'TIRED', 'So sleepy...'),
(-1, 'TIRED', '*eyes drooping*'),
(-1, 'TIRED', 'I need rest...');

-- THIRSTY - When thirsty
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'THIRSTY', '*pants*'),
(-1, 'THIRSTY', 'Water please!'),
(-1, 'THIRSTY', '*looks at water bowl*'),
(-1, 'THIRSTY', 'So thirsty...'),
(-1, 'THIRSTY', '*dry tongue*'),
(-1, 'THIRSTY', 'Need a drink!'),
(-1, 'THIRSTY', '*licks lips*'),
(-1, 'THIRSTY', 'I''m parched!');

-- HUNGRY - When hungry
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'HUNGRY', '*stomach growls*'),
(-1, 'HUNGRY', 'I need food!'),
(-1, 'HUNGRY', '*looks at food bowl*'),
(-1, 'HUNGRY', 'Feed me!'),
(-1, 'HUNGRY', 'So hungry...'),
(-1, 'HUNGRY', '*tummy rumbles*'),
(-1, 'HUNGRY', 'Food please!'),
(-1, 'HUNGRY', '*drools at thought of food*'),
(-1, 'HUNGRY', 'Is it dinner time?'),
(-1, 'HUNGRY', '*sniffs around for food*'),
(-1, 'HUNGRY', 'I could eat a horse!'),
(-1, 'HUNGRY', '*begs for food*'),
(-1, 'HUNGRY', 'Starving over here!');

-- GENERIC_NEUTRAL - Random idle chat
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'GENERIC_NEUTRAL', '*looks around*'),
(-1, 'GENERIC_NEUTRAL', '*sniffs the air*'),
(-1, 'GENERIC_NEUTRAL', '*stretches*'),
(-1, 'GENERIC_NEUTRAL', '*scratches ear*'),
(-1, 'GENERIC_NEUTRAL', '*observes surroundings*'),
(-1, 'GENERIC_NEUTRAL', '*sits quietly*'),
(-1, 'GENERIC_NEUTRAL', '*watches curiously*');

-- GENERIC_SAD - When sad/low happiness
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'GENERIC_SAD', '*whimpers*'),
(-1, 'GENERIC_SAD', '*looks sad*'),
(-1, 'GENERIC_SAD', '*sighs*'),
(-1, 'GENERIC_SAD', '*droops head*'),
(-1, 'GENERIC_SAD', 'I''m lonely...'),
(-1, 'GENERIC_SAD', '*mopes around*'),
(-1, 'GENERIC_SAD', '*looks dejected*'),
(-1, 'GENERIC_SAD', 'Nobody loves me...'),
(-1, 'GENERIC_SAD', '*sulks in corner*');

-- GENERIC_HAPPY - When happy
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(-1, 'GENERIC_HAPPY', '*wags tail happily*'),
(-1, 'GENERIC_HAPPY', '*jumps with joy*'),
(-1, 'GENERIC_HAPPY', ':)'),
(-1, 'GENERIC_HAPPY', 'Life is good!'),
(-1, 'GENERIC_HAPPY', '*prances around*'),
(-1, 'GENERIC_HAPPY', '*does a happy dance*'),
(-1, 'GENERIC_HAPPY', 'I''m so happy!'),
(-1, 'GENERIC_HAPPY', '*beams with joy*'),
(-1, 'GENERIC_HAPPY', 'What a great day!'),
(-1, 'GENERIC_HAPPY', '*grins*'),
(-1, 'GENERIC_HAPPY', '*radiates happiness*'),
(-1, 'GENERIC_HAPPY', 'Yippee!'),
(-1, 'GENERIC_HAPPY', '*spins around happily*'),
(-1, 'GENERIC_HAPPY', 'This is the best!');

-- =====================================================
-- PET-SPECIFIC VOCALS
-- =====================================================

-- Dog (0) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(0, 'GENERIC_HAPPY', 'Woof woof!'),
(0, 'GENERIC_HAPPY', '*wags tail furiously*'),
(0, 'GREET_OWNER', '*barks excitedly*'),
(0, 'GREET_OWNER', 'Woof! You''re home!'),
(0, 'PLAYFUL', '*drops ball at your feet*'),
(0, 'PLAYFUL', 'Throw the ball!'),
(0, 'HUNGRY', '*stares at food bowl*'),
(0, 'DISOBEY', '*chases own tail instead*');

-- Cat (1) specific vocals  
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(1, 'GENERIC_HAPPY', '*purrs*'),
(1, 'GENERIC_HAPPY', 'Meow!'),
(1, 'GENERIC_NEUTRAL', '*grooms self*'),
(1, 'GREET_OWNER', '*rubs against leg*'),
(1, 'DISOBEY', '*looks away disdainfully*'),
(1, 'DISOBEY', '*yawns dismissively*'),
(1, 'PLAYFUL', '*pounces on shadow*'),
(1, 'SLEEPING', '*purrs while sleeping*');

-- Dragon (12) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(12, 'GENERIC_HAPPY', '*breathes small flames happily*'),
(12, 'GENERIC_HAPPY', '*roars softly*'),
(12, 'GENERIC_SAD', '*smoke puffs from nostrils*'),
(12, 'DISOBEY', '*snorts flames*'),
(12, 'DISOBEY', 'I am a DRAGON, not a servant!'),
(12, 'HUNGRY', '*eyes the nearest villager*'),
(12, 'HUNGRY', 'I require sustenance!'),
(12, 'THIRSTY', '*smoke rises as throat dries*'),
(12, 'PLAYFUL', '*chases own tail, breathing fire*'),
(12, 'GREET_OWNER', '*bows majestic head*'),
(12, 'SLEEPING', '*snores, causing small fires*'),
(12, 'LEVEL_UP', '*ROARS triumphantly!*');

-- Horse (15) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(15, 'GENERIC_HAPPY', '*neighs happily*'),
(15, 'GENERIC_NEUTRAL', '*swishes tail*'),
(15, 'GREET_OWNER', '*whinnies in greeting*'),
(15, 'DISOBEY', 'Nay. (Geddit?)'),
(15, 'HUNGRY', '*looks at hay expectantly*'),
(15, 'PLAYFUL', 'Let''s go for a ride!'),
(15, 'TIRED', '*stamps hoof wearily*');

-- Tarantula (8) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(8, 'GREET_OWNER', 'You look more edible every time!'),
(8, 'DISOBEY', '*hisses*'),
(8, 'DISOBEY', 'I do not obey mammals'),
(8, 'HUNGRY', 'Bring me fresh meat!'),
(8, 'PLAYFUL', '*dances on eight legs*'),
(8, 'GENERIC_HAPPY', '*clicks mandibles happily*');

-- Frog (11) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(11, 'GENERIC_HAPPY', 'Ribbit!'),
(11, 'GENERIC_NEUTRAL', '*croaks*'),
(11, 'GREET_OWNER', '*hops excitedly*'),
(11, 'PLAYFUL', '*catches fly with tongue*'),
(11, 'THIRSTY', '*seeks water*');

-- Cow (35) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(35, 'GENERIC_HAPPY', 'Moooo!'),
(35, 'GREET_OWNER', 'Greetings. Did you bring kale?'),
(35, 'EATING', '*chews grass thoughtfully*'),
(35, 'DISOBEY', 'I''d rather meditate'),
(35, 'LEVEL_UP', '*DING* I''m on the up!');

-- Lion (6) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(6, 'GENERIC_HAPPY', '*roars majestically*'),
(6, 'GREET_OWNER', '*nods regally*'),
(6, 'DISOBEY', 'I am the king!'),
(6, 'HUNGRY', '*eyes prey*'),
(6, 'PLAYFUL', '*pounces playfully*');

-- Bear (4) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(4, 'GENERIC_HAPPY', '*growls contentedly*'),
(4, 'HUNGRY', '*sniffs for honey*'),
(4, 'SLEEPING', '*hibernates*'),
(4, 'GREET_OWNER', '*bear hug incoming*');

-- Monkey (14) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(14, 'GENERIC_HAPPY', 'Ook ook!'),
(14, 'PLAYFUL', '*swings from furniture*'),
(14, 'GREET_OWNER', '*does a backflip*'),
(14, 'DISOBEY', '*throws something*'),
(14, 'HUNGRY', '*looks for bananas*');

-- Bunny (17) specific vocals
INSERT INTO `pet_vocals` (`pet_id`, `type`, `message`) VALUES
(17, 'GENERIC_HAPPY', '*hops happily*'),
(17, 'GREET_OWNER', '*twitches nose excitedly*'),
(17, 'PLAYFUL', '*binkies around*'),
(17, 'HUNGRY', '*nibbles on carrot*');

SET FOREIGN_KEY_CHECKS = 1;