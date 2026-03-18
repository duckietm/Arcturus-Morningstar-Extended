-- ============================================================
-- Camera - Database Setup
-- Run this SQL manually before using the camera feature.
-- ============================================================

-- -----------------------------------------
-- Table: camera_web (stores published photos)
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS `camera_web` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `room_id` INT(11) NOT NULL DEFAULT 0,
  `timestamp` INT(11) NOT NULL DEFAULT 0,
  `url` VARCHAR(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  INDEX `idx_camera_web_user_id` (`user_id`),
  INDEX `idx_camera_web_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------
-- Emulator Settings for Camera
-- -----------------------------------------
-- Uses INSERT IGNORE so existing values are not overwritten.

-- Base URL where camera photos are served (include trailing slash)
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('camera.url', 'http://localhost/camera/');

-- Filesystem path where full-size camera photos are saved (include trailing slash)
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('imager.location.output.camera', '/path/to/www/camera/');

-- Filesystem path where room thumbnail images are saved (include trailing slash)
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('imager.location.output.thumbnail', '/path/to/www/thumbnails/');

-- Item ID for the wall photo item (must exist in items_base with interaction type "external_image")
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('camera.item_id', '0');

-- Price in credits to purchase a photo as a wall item
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('camera.price.credits', '2');

-- Price in seasonal points to purchase a photo as a wall item
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('camera.price.points', '0');

-- Price in seasonal points to publish a photo to the web
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('camera.price.points.publish', '1');

-- JSON template for photo item extradata
-- Available placeholders: %timestamp%, %room_id%, %url%, %id%
INSERT IGNORE INTO `emulator_settings` (`key`, `value`) VALUES
('camera.extradata', '{"t":"%timestamp%","u":"%id%","m":"","s":"%room_id%","w":"%url%"}');

-- -----------------------------------------
-- Emulator Texts for Camera
-- -----------------------------------------
INSERT IGNORE INTO `emulator_texts` (`key`, `value`) VALUES
('camera.permission', 'You do not have permission to use the camera.');

INSERT IGNORE INTO `emulator_texts` (`key`, `value`) VALUES
('camera.wait', 'Please wait %seconds% more seconds before taking another photo.');

INSERT IGNORE INTO `emulator_texts` (`key`, `value`) VALUES
('camera.error.creation', 'An error occurred while processing your photo. Please try again.');

INSERT IGNORE INTO `emulator_texts` (`key`, `value`) VALUES
('camera.daily.limit', 'You have reached the daily photo limit. Try again tomorrow.');
