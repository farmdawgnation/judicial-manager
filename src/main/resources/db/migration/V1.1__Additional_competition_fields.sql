alter table `competitions`
  add `location` varchar(255) not null default "",
  add `status` enum('Not Started', 'In Progress', 'Finished') not null default 'Not Started',
  add `round` integer not null default 1;
