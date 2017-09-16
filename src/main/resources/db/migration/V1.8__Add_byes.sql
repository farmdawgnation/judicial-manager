create table `byes` (`id` INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,`competition_id` INTEGER NOT NULL, `team_id` INTEGER NOT NULL,`round` INTEGER NOT NULL,`uuid` BINARY(16) NOT NULL);
alter table `byes` add constraint `b_team_id_fk` foreign key(`team_id`) references `teams`(`id`) on update NO ACTION on delete CASCADE;
alter table `byes` add constraint `b_competition_id_fk` foreign key(`competition_id`) references `competitions`(`id`) on update NO ACTION on delete CASCADE;
