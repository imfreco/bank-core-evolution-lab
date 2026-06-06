create table auth_users (
    id uuid primary key,
    username varchar(80) not null,
    password_hash varchar(120) not null,
    enabled boolean not null,
    created_at timestamp with time zone not null,
    constraint uk_auth_users_username unique (username)
);

create table auth_user_roles (
    user_id uuid not null references auth_users(id) on delete cascade,
    role varchar(40) not null,
    primary key (user_id, role)
);

create index idx_auth_users_username on auth_users(username);
create index idx_auth_user_roles_user_id on auth_user_roles(user_id);

insert into auth_users (id, username, password_hash, enabled, created_at) values
('dddddddd-dddd-dddd-dddd-dddddddddd01', 'customer', '{bcrypt}$2a$10$8G81iGPkEyk/NIgSxR8al.3bxczsw1hNDJvx3o5BZqVdEy05A296e', true, now()),
('dddddddd-dddd-dddd-dddd-dddddddddd02', 'operator', '{bcrypt}$2a$10$pbV/PvDVBRfy0YFSjF8niuy4XitJItuHm0MSvbGudE2n7YYHq9b3i', true, now()),
('dddddddd-dddd-dddd-dddd-dddddddddd03', 'admin', '{bcrypt}$2a$10$5stz1R1GFAY5CR0x2SgCHu6RkgFY4g8bZ8mWle95G/ajV3RGtRaWW', true, now());

insert into auth_user_roles (user_id, role) values
('dddddddd-dddd-dddd-dddd-dddddddddd01', 'CUSTOMER'),
('dddddddd-dddd-dddd-dddd-dddddddddd02', 'OPERATOR'),
('dddddddd-dddd-dddd-dddd-dddddddddd03', 'ADMIN');
