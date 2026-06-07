alter table auth_users
    add column customer_id uuid;

alter table auth_users
    add constraint fk_auth_users_customer
        foreign key (customer_id) references customers(id);

alter table auth_users
    add constraint uk_auth_users_customer_id unique (customer_id);

create index idx_auth_users_customer_id on auth_users(customer_id);

update auth_users
set customer_id = '11111111-1111-1111-1111-111111111111'
where username = 'customer';

insert into auth_users (id, username, password_hash, enabled, customer_id, created_at) values
('dddddddd-dddd-dddd-dddd-dddddddddd04', 'customer2', '{bcrypt}$2a$10$avSk4gY0a5jP9ola5ynWHeC/kOr0lmIaFd3rlThTAhN8w8qYw6qga', true, '22222222-2222-2222-2222-222222222222', now());

insert into auth_user_roles (user_id, role) values
('dddddddd-dddd-dddd-dddd-dddddddddd04', 'CUSTOMER');
