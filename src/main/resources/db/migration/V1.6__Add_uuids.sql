ALTER TABLE `judges` ADD COLUMN `uuid` VARCHAR(36) NULL;
UPDATE `judges` SET uuid = (SELECT uuid());

ALTER TABLE `teams` ADD COLUMN `uuid` VARCHAR(36) NULL;
UPDATE `teams` SET uuid = (SELECT uuid());

ALTER TABLE `matches` ADD COLUMN `uuid` VARCHAR(36) NULL;
UPDATE `matches` SET uuid = (SELECT uuid());
