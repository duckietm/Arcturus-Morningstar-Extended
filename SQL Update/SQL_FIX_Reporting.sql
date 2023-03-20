DROP TABLE IF EXISTS `support_cfh_categories`;
CREATE TABLE IF NOT EXISTS `support_cfh_categories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_internal` varchar(255) DEFAULT NULL,
  `name_external` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

INSERT IGNORE INTO `support_cfh_categories` (`id`, `name_internal`, `name_external`) VALUES
	(1, 'sexual_content', 'seksueel'),
	(2, 'pii_meeting_irl', 'delen persoonsinfo/in het echt afspreken'),
	(3, 'scamming', 'scammen'),
	(4, 'game_interruption', 'trollen en ongepast gedrag'),
	(5, 'trolling_bad_behavior', 'gewelddadig gedrag'),
	(6, 'violent_behavior', 'verstoren van het spel');
	
DROP TABLE IF EXISTS `support_cfh_topics`;
CREATE TABLE IF NOT EXISTS `support_cfh_topics` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category_id` int(11) DEFAULT NULL,
  `name_internal` varchar(255) DEFAULT NULL,
  `name_external` varchar(255) DEFAULT NULL,
  `action` enum('mods','auto_ignore','auto_reply') DEFAULT 'mods',
  `ignore_target` enum('0','1') NOT NULL DEFAULT '0',
  `auto_reply` mediumtext DEFAULT NULL,
  `default_sanction` int(3) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=551 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC;

INSERT IGNORE INTO `support_cfh_topics` (`id`, `category_id`, `name_internal`, `name_external`, `action`, `ignore_target`, `auto_reply`, `default_sanction`) VALUES
	(1, 1, '1', 'Seksueel getint gesprek', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(4, 1, '4', 'Vragen/aanbieden van webcam seks of seksuele afbeeldingen', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(5, 1, '5', 'Vragen/aanbieden van cyberseks', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(6, 2, '6', 'Ontmoeten in real life', 'mods', '0', NULL, 0),
	(8, 2, '8', 'vragen of delen van persoonsgegevens', 'mods', '0', NULL, 0),
	(9, 3, '9', 'Scamsite/retro links', 'mods', '0', NULL, 0),
	(10, 3, '10', 'Kopen of verkopen van meubi/credits/accountgegevens', 'mods', '0', NULL, 0),
	(11, 3, '11', 'Stelen van meubi/credits/accountgegevens', 'mods', '0', NULL, 0),
	(12, 4, '12', 'Beledigen/pesten', 'auto_reply', '0', 'Bedankt voor je verslag. Roep een moderator naar de kamer door deze stappen te volgen. 1. Klik in het Help-venster op \'Onmiddellijk hulp krijgen\'. 2. Klik vervolgens op \'Chat met een moderator\'. 3. Laat ze weten dat iemand in de kamer aan het trollen is. 4. Een moderator opent een chatsessie met u om het probleem op te lossen.', 0),
	(13, 4, '13', 'Ongepaste Camwijsnaam', 'auto_reply', '0', 'Bedankt voor je verslag. Roep een moderator naar de kamer door deze stappen te volgen. 1. Klik in het Help-venster op \'Onmiddellijk hulp krijgen\'. 2. Klik vervolgens op \'Chat met een moderator\'. 3. Laat ze weten dat iemand in de kamer aan het trollen is. 4. Een moderator opent een chatsessie met u om het probleem op te lossen.', 0),
	(14, 4, '14', 'Ongepast taalgebruik', 'mods', '0', NULL, 0),
	(15, 4, '15', 'Drugs promotie', 'mods', '0', NULL, 0),
	(16, 4, '16', 'Gokken', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(17, 4, '17', 'Voordoen als staff', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(18, 4, '18', 'Minderjarige user', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(19, 5, '19', 'Haat zaaien', 'mods', '0', NULL, 0),
	(20, 5, '20', 'Iemand doet aan zelfbeschadiging', 'mods', '0', NULL, 0),
	(21, 5, '21', 'Iemand doet aan zelfbeschadiging', 'mods', '0', NULL, 0),
	(22, 6, '22', 'Opzettelijk verstoren', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(23, 6, '23', 'Blokken', 'auto_reply', '0', 'Bedankt voor je verslag. Roep een moderator naar de kamer door deze stappen te volgen. 1. Klik in het Help-venster op \'Onmiddellijk hulp krijgen\'. 2. Klik vervolgens op \'Chat met een moderator\'. 3. Laat ze weten dat iemand in de kamer aan het trollen is. 4. Een moderator opent een chatsessie met u om het probleem op te lossen.', 0),
	(29, 6, '29', 'Clone Aanvallen', 'mods', '0', NULL, 0),
	(30, 1, '30', 'Seks links', 'auto_ignore', '0', 'Bedankt voor het melden van iemand voor seksuele praat. We hebben deze gebruiker voor je op negeren gezet. Dit betekent dat je niet meer kunt zien wat ze zeggen. Om negeren voor deze persoon uit te schakelen, moet u erop klikken en op \'Luisteren\' drukken. Wij zullen dit bekijken.', 0),
	(32, 3, '32', 'Hack/scam trucjes', 'mods', '0', NULL, 0),
	(33, 3, '33', 'Verdacht van fraude', 'mods', '0', NULL, 0),
	(34, 4, '34', 'Ongepaste kamer/groep/event', 'auto_reply', '0', 'Bedankt voor je verslag. Roep een moderator naar de kamer door deze stappen te volgen. 1. Klik in het Help-venster op \'Onmiddellijk hulp krijgen\'. 2. Klik vervolgens op \'Chat met een moderator\'. 3. Laat ze weten dat iemand in de kamer aan het trollen is. 4. Een moderator opent een chatsessie met u om het probleem op te lossen.', 0),
	(35, 6, '35', 'Scripten', 'mods', '0', NULL, 0);