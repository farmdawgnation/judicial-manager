ALTER TABLE `judges` ADD COLUMN `uuid` BINARY(16) NULL;
UPDATE `judges` SET uuid = (SELECT uuid());

ALTER TABLE `teams` ADD COLUMN `uuid` BINARY(16) NULL;
UPDATE `teams` SET uuid = (SELECT uuid());

ALTER TABLE `matches` ADD COLUMN `uuid` BINARY(16) NULL;
UPDATE `matches` SET uuid = (SELECT uuid());
