alter table `judges` add column `kind` enum('Presiding', 'Scoring') not null default 'Presiding';
