--New permission
ALTER TABLE `permissions` ADD COLUMN `acc_unignorable` ENUM('0','1') NOT NULL DEFAULT '0';
