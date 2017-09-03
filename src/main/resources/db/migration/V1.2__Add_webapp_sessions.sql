create table `webapp_sessions` (`id` CHAR(64) NOT NULL PRIMARY KEY,`user_id` INTEGER NOT NULL);
alter table `webapp_sessions` add constraint `ws_user_id_fk` foreign key(`user_id`) references `users`(`id`) on update NO ACTION on delete CASCADE;
