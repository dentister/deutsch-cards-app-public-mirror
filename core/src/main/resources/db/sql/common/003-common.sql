alter table users add roles text[];

update users set roles = ARRAY['ALL', 'PLAYER', 'LEARNER'];
update users set roles = ARRAY['ALL', 'PLAYER', 'LEARNER', 'ADMIN'] where username = 'sysadm';

alter table users alter column roles SET NOT NULL;

